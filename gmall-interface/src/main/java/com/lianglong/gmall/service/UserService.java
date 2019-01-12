package com.lianglong.gmall.service;

import com.lianglong.gmall.bean.UserAddress;
import com.lianglong.gmall.bean.UserInfo;

import java.util.List;

public interface UserService {

    List<UserInfo> getUserInfoList();

    List<UserAddress> findUserAddressByUserId(String userId);
}
