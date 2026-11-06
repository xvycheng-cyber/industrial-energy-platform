package com.smarten.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarten.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
