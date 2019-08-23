package com.lianglong.gmall.usermanager.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.lianglong.gmall.bean.UserAddress;
import com.lianglong.gmall.bean.UserInfo;
import com.lianglong.gmall.config.RedisUtil;
import com.lianglong.gmall.service.UserService;
import com.lianglong.gmall.usermanager.mapper.UserAddressMapper;
import com.lianglong.gmall.usermanager.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;

import java.util.List;
@Service
public class UserServiceImpl implements UserService {

    public String USERKEY_PREFIX="user:";
    public String USERINFOKEY_SUFFIX=":info";
    public int USERKEY_TIMEOUT=60*60*24;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserAddressMapper userAddressMapper;

    @Override
    public List<UserInfo> getUserInfoList() {


        return userInfoMapper.selectAll();


    }

    @Override
    public List<UserAddress> findUserAddressByUserId(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        List<UserAddress> userAddressList = userAddressMapper.select(userAddress);
        return userAddressList;
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        String passwd = userInfo.getPasswd();
        String newPasswd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(newPasswd);
        UserInfo userInfoSelect = userInfoMapper.selectOne(userInfo);

        if(userInfoSelect!=null){
            Jedis jedis = null;
            try {
                jedis = redisUtil.getJedis();
            } catch (Exception e) {
                e.printStackTrace();
                return userInfoSelect;
            }

            String userInfoKey=USERKEY_PREFIX+userInfoSelect.getId()+USERINFOKEY_SUFFIX;
            jedis.setex(userInfoKey,USERKEY_TIMEOUT,JSON.toJSONString(userInfo));
             jedis.close();
        }

        return userInfoSelect;
    }

    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = redisUtil.getJedis();
        UserInfo userInfo=null;
        String userInfoKey=USERKEY_PREFIX+userId+USERINFOKEY_SUFFIX;

        String userStr = jedis.get(userInfoKey);

        if(userStr!=null&&userStr.length()>0){
            jedis.expire(userInfoKey,USERKEY_TIMEOUT);
            userInfo = JSON.parseObject(userStr, UserInfo.class);

        }
        return userInfo;
    }


}
