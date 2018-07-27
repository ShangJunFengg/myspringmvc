package demo.servlet;

import mvc.annotation.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyDispatcherServiet extends HttpServlet {

    private Properties properties=new Properties();//配置文件
    private List<String> classes=new ArrayList();//包扫描的全限定类名
    private Map<String,Object> beans=new HashMap();//ioc容器
    private ArrayList<Handler> handlermapping=new ArrayList<Handler>();//请求地址映射容器
    @Override
    public void init(ServletConfig config) throws ServletException {
        initConfig(config.getInitParameter("contextConfigLocation"));//初始化配置文件
        initScanner(properties.getProperty("scanPackage"));//初始化包扫描
        initIoc();//初始化ioc容器
        initAutowired();//初始化依赖注入
        initHandlerMapping();//初始化映射关系
        System.out.println("初始化完成");
    }

    /**
     * 初始化映射关系
     */
    private void initHandlerMapping() {
        if (beans.isEmpty()) { return; }

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(MyContorller.class)){continue;}
            String url ="";
            if(clazz.isAnnotationPresent(MyRequestMapping.class))
            {
                url=clazz.getAnnotation(MyRequestMapping.class).value();
                System.out.println("扫描到"+clazz.getName()+"类上面的请求地址映射:"+url);
            }

            //拿到所有方法
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if(!method.isAnnotationPresent(MyRequestMapping.class)){continue;};
                String methodUrl = method.getAnnotation(MyRequestMapping.class).value();
                String reg=("/"+url+methodUrl).replaceAll("/+","/");
                Pattern pattern = Pattern.compile(reg);
                handlermapping.add( new Handler(entry.getValue(), method, pattern));
                System.out.println("扫描到"+clazz.getName()+"里面的"+method.getName()+"方法的请求地址映射:"+methodUrl);
                System.out.println(reg+"已添加到请求地址映射容器");

            }

        }
    }

    /**
     * 初始化依赖注入
     */
    private void initAutowired() {
        if (beans.isEmpty()) { return; }
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            //当前类的所有字段
            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
            for (Field field: declaredFields)
            {
                if(field.isAnnotationPresent(MyAutowired.class))//如果有装配注解
                {
                    String beanName = field.getAnnotation(MyAutowired.class).value().trim();
                    if("".equals(beanName))
                    {
                         beanName = field.getType().getName();
                    }
                    field.setAccessible(true);
                    try {
                        System.out.println("开始向"+entry.getValue()+"注入"+beanName);
                        field.set(entry.getValue(),beans.get(beanName));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        continue;
                    }

                }
            }

        }

    }


    /**
     * 初始化ioc容器
     */
    private void initIoc() {
        if (classes.isEmpty())
        {
            return;
        }
        try {
            for (String className:classes)
            {
                System.out.println("正在装配"+className);
                Class<?> clazz = Class.forName(className);
                    if(clazz.isAnnotationPresent(MyContorller.class))
                    {
                        String beanName = lowsercase(className);
                        Object bean = clazz.newInstance();
                        beans.put(beanName,bean);
                    }else if(clazz.isAnnotationPresent(MyServer.class))
                    {
                        MyServer myServer = clazz.getAnnotation(MyServer.class);

                        String beanName = myServer.value();
                        if("".equals(beanName))
                        {
                            beanName = lowsercase(className);
                        }

                        Object bean = clazz.newInstance();
                        beans.put(beanName,bean);

                        //接口
                        Class<?>[] interfaces = clazz.getInterfaces();
                        for (Class<?> i : interfaces)
                        {
                         beans.put(i.getName(),bean);
                        }

                    }else
                    {
                        continue;
                    }

            }
        }catch (Exception e)
        {e.printStackTrace();}

    }



    /**
     * 类名小写
     * @param str
     * @return
     */
    public String lowsercase(String str)
    {
        char[] chars = str.toCharArray();
            if( 65<= chars[0] && 90 >= chars[0])
            {
                chars[0]+=32;
                return String.valueOf(chars);
            }
        return str;
    }


    /**
     * 初始化包扫描
     * @param scanPackage
     */
    private void initScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource(scanPackage.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());

        for (File file :dir.listFiles())
        {
            if(file.isDirectory())//如果是文件夹
            {
                initScanner(scanPackage+"."+file.getName());
            }else
            {
                classes.add(scanPackage+ "."+(file.getName().replaceAll(".class","")));
                System.out.println("扫描到"+scanPackage+ "."+(file.getName().replaceAll(".class","")));
            }
        }

    }


    /**
     * 初始化配置文件
     */
    private void initConfig(String contextConfigLocaltion) {
        InputStream inputStream = this.getClass().getResourceAsStream(contextConfigLocaltion);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doHandler(req,resp);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }


    public void doHandler(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        Handler handler = patternGandler(req);

        if(handler==null)
        {
            resp.setContentType("text/html;charset=utf-8");
            resp.getWriter().write("页面不见了....");
            return;
        }

            Class<?>[] types = handler.method.getParameterTypes();
            Object[] valus = new Object[types.length];

            //拿到url携带的数据
            Map<String, String[]> parameterMap = req.getParameterMap();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");

                if(!handler.paramIndex.containsKey(entry.getKey()))
                {
                    continue;
                }

                Integer index = handler.paramIndex.get(entry.getKey());
                valus[index]= convert(types[index], value);
            }

        Integer reqindex = handler.paramIndex.get(HttpServletRequest.class.getName());
        Integer respindex = handler.paramIndex.get(HttpServletResponse.class.getName());

        valus[reqindex]=req;
        valus[respindex]=resp;
        handler.method.invoke(handler.contorller,valus);

    }

public Object convert(Class<?> type , String value)
{
    if(Integer.class== type)
    {
        return Integer.valueOf(value);
    }
    return value;
}


    /**
     *
     根据url匹配对应的handler
     */
    public Handler patternGandler(HttpServletRequest req)
    {
        if(handlermapping.isEmpty()){return null;}
        String url = req.getRequestURI();
        String path = req.getContextPath();
        url= url.replace(path,"");
        for (Handler handler : handlermapping) {
            Matcher matcher = handler.pattern.matcher(url);
            if(!matcher.matches()){
                continue;
            }
            return handler;
        }
        return null;
    }


    /**
     * 存放contorller映射关系容器
     */
    private class Handler
    {
        Object contorller;
        Method method;
        Pattern pattern;
        HashMap<String,Integer> paramIndex;
        private Handler(){};
        public Handler(Object contorller, Method method, Pattern pattern) {
            this.contorller = contorller;
            this.method = method;
            this.pattern = pattern;
            paramIndex=new HashMap<String, Integer>();
            putParamIndex(method);
        }

        public void putParamIndex( Method method)
        {
            Annotation[][] annotations = method.getParameterAnnotations();
            for (int i = 0; i < annotations.length; i++) {
                for (Annotation annotation : annotations[i]) {
                    if(annotation instanceof MyRequestParam)
                    {
                        String paraName = ((MyRequestParam) annotation).value();
                        if(!"".equals(paraName))
                        {
                            paramIndex.put(paraName,i);
                            System.out.println("扫描到"+method.getName()+"方法的注解参数"+paraName);
                        }

                    }
                }
            }

            Class<?>[] types = method.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                if(types[i] == HttpServletRequest.class || types[i] == HttpServletResponse.class)
                {
                    paramIndex.put(types[i].getName(),i);
                }
            }

        }

    }


}



