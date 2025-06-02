package xyz.kbws.weixin.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import xyz.kbws.weixin.common.MessageTextEntity;
import xyz.kbws.weixin.common.SignatureUtil;
import xyz.kbws.weixin.common.XmlUtil;

import java.util.Map;

/**
 * @author kbws
 * @date 2025/6/2
 * @description:
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/weixin/portal/")
public class WeixinPortalController {

    @Value("${weixin.config.originalId}")
    private String originalId;

    @Value("${weixin.config.token}")
    private String token;

    @GetMapping(value = "receive", produces = "text/plain;charset=utf-8")
    public String validate(@RequestParam(value = "signature", required = false) String signature,
                           @RequestParam(value = "timestamp", required = false) String timestamp,
                           @RequestParam(value = "nonce", required = false) String nonce,
                           @RequestParam(value = "echostr", required = false) String echostr) {
        try {
            log.info("微信公众号验签信息开始: [{}, {}, {}, {}]", signature, timestamp, nonce, echostr);
            if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
                throw new IllegalArgumentException("请求参数非法，请核实!");
            }
            boolean check = SignatureUtil.check(token, signature, timestamp, nonce);
            log.info("微信公众号验签信息完成 check: {}", check);
            if (!check) {
                return null;
            }
            return echostr;
        } catch (Exception e) {
            log.info("微信公众号验签信息失败: [{}, {}, {}, {}]", signature, timestamp, nonce, echostr);
            return null;
        }
    }

    @PostMapping(value = "receive", produces = "application/xml; charset=UTF-8")
    public String post(@RequestBody String requestBody,
                       @RequestParam("signature") String signature,
                       @RequestParam("timestamp") String timestamp,
                       @RequestParam("nonce") String nonce,
                       @RequestParam("openid") String openid,
                       @RequestParam(name = "encrypt_type", required = false) String encType,
                       @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        try {
            log.info("接收微信公众号信息请求{}开始 {}", openid, requestBody);
            // 消息转换
            MessageTextEntity message = XmlUtil.xmlToBean(requestBody, MessageTextEntity.class);
            String res = buildMessageTextEntity(openid, "你好，" + message.getContent());
            log.info("返回消息: {}", res);
            return res;
        } catch (Exception e) {
            log.info("接收微信公众号信息请求{}失败 {}", openid, requestBody, e);
            return "";
        }
    }

    /**
     * 调试接口 - 用于接收微信回调的所有参数并记录
     * 测试方式；
     * 1. 修改 receive/debug 为 receive，另外一个 receive 修改为 receive2
     * 2. debug 调试运行，看 requestParams 的入参信息
     */
    @PostMapping(value = "receive/debug", produces = "application/xml; charset=UTF-8")
    public String debugParams(@RequestBody String requestBody,
                              @RequestParam Map<String, String> requestParams) {
        try {
            // 记录所有请求参数
            log.info("微信调试接口 - 请求参数Map: {}", requestParams);
            log.info("微信调试接口 - 请求体: {}", requestBody);

            // 消息转换
            MessageTextEntity message = XmlUtil.xmlToBean(requestBody, MessageTextEntity.class);
            return buildMessageTextEntity(requestParams.get("openid"), "你好，" + message.getContent());

        } catch (Exception e) {
            log.error("微信调试接口处理失败", e);
            return "";
        }
    }

    private String buildMessageTextEntity(String openid, String content) {
        MessageTextEntity res = new MessageTextEntity();
        // 公众号分配的ID
        res.setFromUserName(originalId);
        res.setToUserName(openid);
        res.setCreateTime(String.valueOf(System.currentTimeMillis() / 1000L));
        res.setMsgType("text");
        res.setContent(content);
        return XmlUtil.beanToXml(res);
    }

}
