package com.lianglong.gmall.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class FileUploadController {
    @Value("${fileServer.url}") // fileUrl=http://192.168.67.212
    private String fileUrl;
    // springmvc : 文件上传的时候， MultipartFile files
    /*
        <from method="post" action="fileUpload" enstype=multipart/data-format>
            <input type="file" name="files">
            <input type="submit" value="提交">
        </from>
       springmvc 传参一种 @RequestParam ：
     */

    @RequestMapping("fileUpload")
    public String fileUpload(@RequestParam("file") MultipartFile file) throws IOException, MyException {
        String imgUrl=fileUrl;
        if (file!=null){
            String configFile  = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            TrackerClient trackerClient=new TrackerClient();
            TrackerServer trackerServer=trackerClient.getConnection();
            StorageClient storageClient=new StorageClient(trackerServer,null);

            // 如何获取文件名称
            String originalFilename = file.getOriginalFilename();
            // 如何获取后缀名
            String extName  = StringUtils.substringAfterLast(originalFilename, ".");
//            String orginalFilename="e://img//zly.jpg";
            String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];
//                System.out.println("s = " + s);
                // http://192.168.67.212
                imgUrl+="/"+path;
                //			s = group1
                //			s = M00/00/00/wKhD1Fw8scKAdvEXAACGx2c4tJ4858.jpg
            }
        }

        // 实现软编码，将重要信息放入配置文件中！

        // http://192.168.67.212
        // http://192.168.67.212/group1/M00/00/00/wKhD1Fw8scKAdvEXAACGx2c4tJ4858.jpg
        // return "https://m.360buyimg.com/babel/jfs/t5137/20/1794970752/352145/d56e4e94/591417dcN4fe5ef33.jpg";
        System.out.println(imgUrl);
        return imgUrl;
    }
}

