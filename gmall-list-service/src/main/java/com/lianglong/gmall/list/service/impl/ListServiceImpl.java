package com.lianglong.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.lianglong.gmall.bean.SkuLsInfo;
import com.lianglong.gmall.bean.SkuLsParams;
import com.lianglong.gmall.bean.SkuLsResult;
import com.lianglong.gmall.config.RedisUtil;
import com.lianglong.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {
   
    @Autowired
    JestClient jestClient;
    @Autowired
    RedisUtil redisUtil;

    public static final String ES_INDEX = "gmall";

    public static final String ES_TYPE = "SkuInfo";


    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {
        //保存数据到es
        //上架 将最新的产品新增到es上
        //下架 将商品从es中删除
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

        try {
            jestClient.execute(index);//执行保存
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        //构建sdl 查巡关键字
        String query = makeQueryStringForSearch(skuLsParams);
        Search action = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(action);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //处理结果集
        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams, searchResult);

        return skuLsResult;
    }

    @Override
    public void incrHotScore(String skuId) {
        Jedis jedis = redisUtil.getJedis();
        int timesToEs=10;
        Double hotScore = jedis.zincrby("hotScore", 1, "skuId:" + skuId);
        if(hotScore%timesToEs==0){
            updateHotScore(skuId,  Math.round(hotScore));
        }

    }

    private void updateHotScore(String skuId, long hotScore) {
        String updateJson="{\n" +
                "   \"doc\":{\n" +
                "     \"hotScore\":"+hotScore+"\n" +
                "   }\n" +
                "}";

        Update update = new Update.Builder(updateJson).index("gmall").type("SkuInfo").id(skuId).build();
        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {

        SkuLsResult skuLsResult = new SkuLsResult();

        List<SkuLsInfo> skuLsInfoList = new ArrayList<>(skuLsParams.getPageSize());

        //获取sku列表
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);

        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {

            SkuLsInfo skuLsInfo = hit.source;

            if (hit.highlight != null && hit.highlight.size() > 0) {

                List<String> list = hit.highlight.get("skuName");

                //把带有高亮标签的字符串替换skuName
                String skuNameHl = list.get(0);

                skuLsInfo.setSkuName(skuNameHl);
            }
            skuLsInfoList.add(skuLsInfo);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoList);

        skuLsResult.setTotal(searchResult.getTotal());

        //取记录个数并计算出总页数
        long totalPage = (searchResult.getTotal() + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize();

        skuLsResult.setTotalPages(totalPage);

        //取出涉及的属性值id
        List<String> attrValueIdList = new ArrayList<>();

        MetricAggregation aggregations = searchResult.getAggregations();

        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");

        if (groupby_attr != null) {

            List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();

            for (TermsAggregation.Entry bucket : buckets) {

                attrValueIdList.add(bucket.getKey());

            }
            skuLsResult.setAttrValueIdList(attrValueIdList);
        }
        return skuLsResult;

    }

    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (skuLsParams.getKeyword() != null) {

            MatchQueryBuilder ma = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());

            boolQueryBuilder.must(ma);

            // 设置高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            // 设置高亮字段
            highlightBuilder.field("skuName");

            highlightBuilder.preTags("<span style='color:red'>");

            highlightBuilder.postTags("</span>");
            // 将高亮结果放入查询器中
            searchSourceBuilder.highlight(highlightBuilder);

        }
        // 设置三级分类
        if (skuLsParams.getCatalog3Id() != null) {

            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());

            boolQueryBuilder.filter(termQueryBuilder);

        }
        // 设置属性值
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {

            for (int i = 0; i < skuLsParams.getValueId().length; i++) {

                String valueId = skuLsParams.getValueId()[i];

                TermQueryBuilder termsQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);

                boolQueryBuilder.filter(termsQueryBuilder);
            }
        }
        searchSourceBuilder.query(boolQueryBuilder);
        // 设置分页
        int form = (skuLsParams.getPageNo() - 1) * skuLsParams.getPageSize();

        searchSourceBuilder.from(form);

        searchSourceBuilder.size(skuLsParams.getPageSize());
        // 设置按照热度
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        // 设置聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");

        searchSourceBuilder.aggregation(groupby_attr);

        String query = searchSourceBuilder.toString();

        System.out.println("query=" + query);

        return query;

    }

}
