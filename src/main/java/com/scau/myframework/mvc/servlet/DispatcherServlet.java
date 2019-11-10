package com.scau.myframework.mvc.servlet;

import com.google.gson.Gson;
import com.scau.myframework.mvc.annotation.*;
import com.scau.myframework.mvc.entity.ModelAndView;
import com.scau.myframework.mvc.helper.ClassHelper;
import com.scau.myframework.mvc.helper.IocHelper;

import com.scau.myframework.mvc.util.PropertiesUtils;
import com.scau.myframework.test.controller.UserController;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


public class DispatcherServlet extends HttpServlet {



    private Map<String,Object> beanMap;
    Map<String,Object> handlerMap ;

    @Override
    public void init(ServletConfig config) throws ServletException {

        //TODO 获取IOC容器，后期应该通过ioc模块完成此功能而不是IocHelper（IocHelper只能完成mvc的依赖注入逻辑）
        beanMap =  IocHelper.getBeanMap();

        //将请求路径与对应的处理方法映射起来(即：解析url和Method的关联关系)
        initHandlerMappings();

        //TODO 处理静态资源，JSP等情况
    }

    private  void initHandlerMappings() {
        handlerMap = new HashMap<String,Object>();
        for (Class<?> clazz : ClassHelper.getControllerClass()) {
            MyRequestMapping typeMapping = clazz.getAnnotation(MyRequestMapping.class);

            Method[] methods = clazz.getMethods();
            for(Method method:methods){
                if(method.isAnnotationPresent(MyRequestMapping.class)){

                    MyRequestMapping methodMapping = method.getAnnotation(MyRequestMapping.class);
                    String requestPath = typeMapping.value() + methodMapping.value();
                    handlerMap.put(requestPath,method);
                }else{
                    continue;
                }
            }
        }
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String requestPath = uri.replace(contextPath,"");

        handle(req, resp, requestPath);
    }

    //根据传入的url,利用反射，完成请求处理
    //只能为带有@MyRequestParam注解的参数赋值，但只是用Object存储，没有进行类型转换
    //后期还要考虑是否要给对象类型的参数自动注入
    private  void handle(HttpServletRequest req, HttpServletResponse resp, String  requestPath){

        Method method = (Method) handlerMap.get(requestPath);

        Class<?>[] paramClazzs = method.getParameterTypes();

        //TODO 这里的参数没有经过类型转换，都是string类型
        Object[] args = new Object[paramClazzs.length];

        int args_i = 0;
        int index = 0;
        for (Class<?> paramClazz:paramClazzs){
            if(ServletRequest.class.isAssignableFrom(paramClazz)){
                args[args_i++] = req;
            }
            if(ServletResponse.class.isAssignableFrom(paramClazz)){
                args[args_i++] = resp;
            }
            Annotation[] paramAns = method.getParameterAnnotations()[index];
            if (paramAns.length > 0){
                for (Annotation paramAn:paramAns){
                    if(MyRequestParam.class.isAssignableFrom(paramAn.getClass())){
                        MyRequestParam myRequestParam = (MyRequestParam) paramAn;
                        args[args_i++] = req.getParameter(myRequestParam.value());
                    }
                }
            }
            index++;
        }
        Object instance = beanMap.get("/" + requestPath.split("/")[1]);

        Object result = null;

        try {
            result = method.invoke(instance, args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        if(null == result) {
            return;
        }

        if (result instanceof String) {

            try {
                if (((String) result).startsWith("redirect:")){
                    resp.sendRedirect(req.getContextPath()+((String) result).replace("redirect:","").trim());
                }
                req.getRequestDispatcher(PropertiesUtils.getJspPath()+result).forward(req, resp);
            } catch (Exception e) {
            }
            return;
        } else if (result instanceof ModelAndView){

            ModelAndView mv = (ModelAndView) result;
            if (mv.getPath() != null) {
                try {
                    if (mv.getPath().startsWith("redirect:")){
                        resp.sendRedirect(req.getContextPath()+mv.getPath().replace("redirect:","").trim());
                        return;
                    }
                    Map<String, Object> model = mv.getModels();
                    for(Map.Entry<String, Object> entry:model.entrySet()){
                        req.setAttribute(entry.getKey(), entry.getValue());
                    }
                    req.getRequestDispatcher(PropertiesUtils.getJspPath()+mv.getPath()).forward(req, resp);
                } catch (Exception e) {

                }
            }
        } else{//其他情况统一返回json数据，（当然包括标注了@MyResponseBody注解的）
            try {
                Gson gson = new Gson();
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                PrintWriter writer = resp.getWriter();
                writer.write(gson.toJson(result));
                writer.flush();
                writer.close();
            } catch (Exception e) {
            }
            return;
        }




    }

}
