package com.lianglong.gmall.usermanager.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.lianglong.gmall.bean.UserAddress;
import com.lianglong.gmall.bean.UserInfo;
import com.lianglong.gmall.service.UserService;
import com.lianglong.gmall.usermanager.mapper.UserAddressMapper;
import com.lianglong.gmall.usermanager.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
@Service
public class UserServiceImpl implements UserService {

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


}
