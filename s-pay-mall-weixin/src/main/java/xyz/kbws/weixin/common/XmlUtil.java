package xyz.kbws.weixin.common;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.Writer;
import java.util.*;

/**
 * @author kbws
 * @date 2025/6/2
 * @description:
 */
public class XmlUtil {

    /**
     * 解析微信发来的请求(xml)
     * @param request
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> xmlToMap(HttpServletRequest request) throws Exception {
        // 从 request 中取得输入流
        try (InputStream inputStream = request.getInputStream()) {
            // 将解析结果存储到 map 中
            Map<String, String> map = new HashMap<>();
            // 读取输入流
            SAXReader reader = new SAXReader();
            // 得到 xml 文档
            Document document = reader.read(inputStream);
            // 得到 xml 根元素
            Element root = document.getRootElement();
            // 得到根元素的所有子节点
            List<Element> elementList = root.elements();
            // 遍历所有子节点
            for (Element element : elementList) {
                map.put(element.getName(), element.getText());
            }
            // 释放资源
            inputStream.close();
            return map;
        }
    }

    private static void mapToXml2(Map map, StringBuffer sb) {
        Set set = map.keySet();
        for (Object o : set) {
            String key = (String) o;
            Object value = map.get(key);
            if (value == null) {
                value = "";
            }
            if (value.getClass().getName().equals("java.util.ArrayList")) {
                ArrayList list = (ArrayList) map.get(key);
                sb.append("<").append(key).append(">");
                for (Object o1 : list) {
                    HashMap hm = (HashMap) o1;
                    mapToXml2(hm, sb);
                }
                sb.append("</").append(key).append(">");
            } else {
                if (value instanceof HashMap) {
                    sb.append("<").append(key).append(">");
                    mapToXml2((HashMap) value, sb);
                    sb.append("</").append(key).append(">");
                } else {
                    sb.append("<").append(key).append("><![CDATA[").append(value).append("]]></").append(key).append(">");
                }
            }
        }
    }

    /**
     * bean 转换成微信的 xml 格式
     * @param object
     * @return
     */
    public static String beanToXml(Object object) {
        XStream stream = getMyStream();
        stream.alias("xml", object.getClass());
        stream.processAnnotations(object.getClass());
        return stream.toXML(object);
    }

    public static XStream getMyStream() {
        return new XStream(new XppDriver() {
            @Override
            public HierarchicalStreamWriter createWriter(Writer out) {
                return new PrettyPrintWriter(out) {
                    // 对所有 xml 节点都增加 CDATA 标记
                    boolean cdata = true;

                    @Override
                    public void startNode(String name, Class clazz) {
                        super.startNode(name, clazz);
                    }

                    @Override
                    protected void writeText(QuickWriter writer, String text) {
                        if (cdata && !StringUtils.isNumeric(text)) {
                            writer.write("<![CDATA[");
                            writer.write(text);
                            writer.write("]]>");
                        } else {
                            writer.write(text);
                        }
                    }
                };
            }
        });
    }

    /**
     * xml 转成 bean 方法
     * @param resultXml
     * @param clazz
     * @return
     * @param <T>
     */
    public static <T> T xmlToBean(String resultXml, Class clazz) {
        // XStream 对象设置默认安全防护，同事设置允许的类
        XStream stream = new XStream(new DomDriver());
        stream.addPermission(AnyTypePermission.ANY);
        XStream.setupDefaultSecurity(stream);
        stream.allowTypes(new Class[]{clazz});
        stream.processAnnotations(new Class[]{clazz});
        stream.setMode(XStream.NO_REFERENCES);
        stream.alias("xml", clazz);
        return (T) stream.fromXML(resultXml);
    }
}
