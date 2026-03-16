package com.campus.courier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.courier.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {}
