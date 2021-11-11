package com.voidtech.module_ble.common;

/**
 * @Description: 属性类型
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/17 20:27
 */
public enum PropertyType {
    PROPERTY_READ(0x01),
    PROPERTY_WRITE(0x02),
    PROPERTY_NOTIFY(0x04),
    PROPERTY_INDICATE(0x08); // 有应答的通知特征，收到ble的消息，主机会通知ble已收到

    private int propertyValue;

    PropertyType(int propertyValue) {
        this.propertyValue = propertyValue;
    }

    public int getPropertyValue() {
        return propertyValue;
    }
}
