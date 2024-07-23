package com.sangtacviet.capacitorwebnative;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import android.content.Context;
import android.view.View;

import com.getcapacitor.JSObject;

public class PViewManager {
    ArrayList<View> viewList = new ArrayList<>();
    ArrayList<ViewPass> viewInfoList = new ArrayList<>();
    ArrayList<ObjectPass> objectInfoList = new ArrayList<>();
    ArrayList<Object> objectList = new ArrayList<>();
    public ClassLoader classLoader;
    Context context;
    public PViewManager(ClassLoader cl, Context ct){
        this.classLoader = cl;
        this.context = ct;
    }
    public static class CreateViewError{
        public static int ClassNotFound = -1,
        NewInstanceError = -2,
        NotAView = -3;
    }
    public int createView(String className){
        var retNum = viewList.size();
        Class viewClass = null;
        try {
            viewClass = classLoader.loadClass(className);
            if(!View.class.isAssignableFrom(viewClass)){
                return CreateViewError.NotAView;
            }
        }catch (Exception e){
            return CreateViewError.ClassNotFound;
        }
        View viewObj = null;
        try {
            viewObj = (View)viewClass.getConstructor(Context.class).newInstance(context);
        }catch (Exception e){
            return CreateViewError.NewInstanceError;
        }
        var viewInfo = new ViewPass();
        viewInfo.methodInfos = getMethodList(viewClass, true);
        viewInfo.propertyInfos = getPropertyList(viewClass);
        viewInfo.propertyInfos.addAll(getPropertyList(View.class));
        viewInfo.viewId = retNum;
        viewInfo.className = className;
        //viewObj.setDrawingCacheEnabled(true);
        var obs = viewObj.getViewTreeObserver();
        obs.addOnDrawListener(() -> {
            WebNativeViewPlugin.instance.onUpdate(retNum);
        });
        viewList.add(viewObj);
        viewInfoList.add(viewInfo);
        View finalViewObj = viewObj;
        WebNativeViewPlugin.mainActivity.runOnUiThread(() -> {
            WebNativeViewPlugin.holder.addView(finalViewObj, 100,30);
            finalViewObj.setVisibility(View.GONE);
        });

        return retNum;
    }
    public int createObject(String className){
        var retNum = objectList.size();
        Class viewClass = null;
        try {
            viewClass = classLoader.loadClass(className);
        }catch (Exception e){
            return CreateViewError.ClassNotFound;
        }
        Object viewObj = null;
        try {
            viewObj = viewClass.getConstructor().newInstance();
        }catch (Exception e){
            return CreateViewError.NewInstanceError;
        }
        var viewInfo = new ObjectPass();
        viewInfo.methodInfos = getMethodList(viewClass, true);
        viewInfo.propertyInfos = getPropertyList(viewClass);
        viewInfo.viewId = retNum;
        viewInfo.className = className;
        objectList.add(viewObj);
        objectInfoList.add(viewInfo);
        return retNum;
    }
    public int statObject(Object viewObj){
        var retNum = objectList.size();
        Class viewClass = viewObj.getClass();
        var viewInfo = new ObjectPass();
        viewInfo.methodInfos = getMethodList(viewClass, true);
        viewInfo.propertyInfos = getPropertyList(viewClass);
        viewInfo.viewId = retNum;
        viewInfo.className = viewClass.getName();
        objectList.add(viewObj);
        objectInfoList.add(viewInfo);
        return retNum;
    }
    public ArrayList<MethodInfo> getMethodList(Class clazz, Boolean isRecur){
        var ret = new ArrayList<MethodInfo>();
        var mets = clazz.getDeclaredMethods();
        for(Method met : mets){
            var methodInfo = new MethodInfo();
            methodInfo.name = met.getName();
            var modifierInt = met.getModifiers();
            methodInfo.isPublic = Modifier.isPublic(modifierInt);
            methodInfo.isStatic = Modifier.isStatic(modifierInt);
            methodInfo.returnType = met.getReturnType().getName();
            methodInfo.paramCount = met.getParameterTypes().length;
            methodInfo.refer = met;
            if(methodInfo.isPublic){
                ret.add(methodInfo);
            }
        }
        if(isRecur && clazz.getName() != "android.view.View"){
            clazz = clazz.getSuperclass();
            while (clazz != null && clazz.getName() != "android.view.View"){
                ret.addAll(getMethodList(clazz,false));
                clazz = clazz.getSuperclass();
            }
            if(clazz != null)ret.addAll(getMethodList(clazz,false));
        }
        return ret;
    }
    public ArrayList<PropertyInfo> getPropertyList(Class clazz){
        var ret = new ArrayList<PropertyInfo>();
        var fields = clazz.getDeclaredFields();
        for(Field field : fields){
            var fieldInfo = new PropertyInfo();
            fieldInfo.name = field.getName();
            var modifierInt = field.getModifiers();
            fieldInfo.isPublic = Modifier.isPublic(modifierInt);
            fieldInfo.isStatic = Modifier.isStatic(modifierInt);
            fieldInfo.valueType = field.getClass().getName();
            fieldInfo.refer = field;
            ret.add(fieldInfo);
        }
        return ret;
    }
    public String invokeMethod(int viewId, int methodId, ArrayList params){
        var view = viewList.get(viewId);
        var viewInfo = viewInfoList.get(viewId);
        var met = viewInfo.methodInfos.get(methodId);
        if(met.isPublic){
            try {
                return Util.invokeMethod(view,met.refer,params);
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        return "Method not accessible";
    }
    public String invokeObjectMethod(int viewId, int methodId, ArrayList params){
        var view = objectList.get(viewId);
        var viewInfo = objectInfoList.get(viewId);
        var met = viewInfo.methodInfos.get(methodId);
        if(met.isPublic){
            try {
                return Util.invokeMethod(view,met.refer,params);
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        return "Method not accessible";
    }
    public class MethodInfo {
        String name,returnType;
        Boolean isPublic,isStatic;
        int paramCount = 0;
        Method refer;
        public JSObject serialize(){
            var object = new JSObject();
            object.put("name",name);
            object.put("isPublic",isPublic);
            object.put("isStatic",isStatic);
            object.put("returnType",returnType);
            object.put("paramCount",paramCount);
            return object;
        }
    }
    public class PropertyInfo {
        String name, valueType;
        Boolean isPublic,isStatic;
        Field refer;
        public JSObject serialize(){
            var object = new JSObject();
            object.put("name",name);
            object.put("isPublic",isPublic);
            object.put("isStatic",isStatic);
            object.put("valueType",valueType);
            return object;
        }
    }
    public class ViewPass {
        int viewId;
        String className;
        ArrayList<MethodInfo> methodInfos;
        ArrayList<PropertyInfo> propertyInfos;
    }
    public class ObjectPass {
        int viewId;
        String className;
        ArrayList<MethodInfo> methodInfos;
        ArrayList<PropertyInfo> propertyInfos;
    }
    public void Test(){
    }
}