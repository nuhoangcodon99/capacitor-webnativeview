package com.sangtacviet.capacitorwebnative;

import static java.io.File.separator;

import android.graphics.Bitmap;
import android.util.Base64;
import android.view.View;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Util {
    public static boolean isWrapperType(Object obj)
    {
        if(obj == null) return false;
        // check if clazz.toString is the Object.toString
        // if so, it's a wrapper type
        var objHashCode = obj.getClass().getName() + "@" + Integer.toHexString(obj.hashCode());
        return !obj.toString().equals(objHashCode);
    }

    public static String invokeMethod(Object obj, Method met, ArrayList<String> params){
        var metParams = met.getParameterTypes();
        if(metParams.length != params.size()){
            return "Param count not match";
        }
        var av = new Object[metParams.length];
        for(var i=0;i<metParams.length;i++){
            var cl = metParams[i];
            var cln = cl.getName();
            var vl = params.get(i);
            if(cln == "java.lang.Boolean" || cln == boolean.class.getName() || cln =="boolean"){
                av[i] = Boolean.valueOf(vl);
            }else
            if(cln == "java.lang.String" || cln == CharSequence.class.getName()){
                av[i] = vl;
            }else
            if(cln == "java.lang.Integer" ||cln == int.class.getName()){
                av[i] = Integer.parseInt(vl);
            }else
            if(cln == "java.lang.Float"||cln == float.class.getName()){
                av[i] = Float.parseFloat(vl);
            }else
            if(cln == "java.lang.Double"||cln == double.class.getName()){
                av[i] = Double.parseDouble(vl);
            }else
            if(cln == "java.lang.Long"||cln == long.class.getName()){
                av[i] = Long.parseLong(vl);
            }else if(View.class.isAssignableFrom(cl)){
                av[i] = WebNativeViewPlugin.instance.viewManager.viewList.get(Integer.parseInt(vl));
            }else
            if(cl.isEnum()){
                var enums = cl.getEnumConstants();
                Object rvl = null;
                for(var e : enums){
                    if(e.toString() == vl){
                        rvl = e;
                        break;
                    }
                }
                if(rvl == null){
                    return "Cannot find specified ENUM";
                }
                av[i] = rvl;
            }else{
                try{
                    var t = Integer.parseInt(vl);
                    if(WebNativeViewPlugin.instance.viewManager.objectList.size() > t){
                        av[i] = WebNativeViewPlugin.instance.viewManager.objectList.get(t);
                    }else{
                        return "Cannot find specified object at " +i+" as "+cln;
                    }
                }catch(Exception e){
                    return "Not supported value Type at " +i+" as "+cln;
                }
            }
        }
        Object result = null;
        try {
            switch (metParams.length){
                case 0:result=met.invoke(obj);break;
                case 1:result=met.invoke(obj,av[0]);break;
                case 2:result=met.invoke(obj,av[0],av[1]);break;
                case 3:result=met.invoke(obj,av[0],av[1],av[2]);break;
                case 4:result=met.invoke(obj,av[0],av[1],av[2],av[3]);break;
                case 5:result=met.invoke(obj,av[0],av[1],av[2],av[3],av[4]);break;
                case 6:result=met.invoke(obj,av[0],av[1],av[2],av[3],av[4],av[5]);break;
                case 7:result=met.invoke(obj,av[0],av[1],av[2],av[3],av[4],av[5],av[6]);break;
                case 8:result=met.invoke(obj,av[0],av[1],av[2],av[3],av[4],av[5],av[6],av[7]);break;
                case 9:result=met.invoke(obj,av[0],av[1],av[2],av[3],av[4],av[5],av[6],av[7],av[8]);break;
                case 10:result=met.invoke(obj,av[0],av[1],av[2],av[3],av[4],av[5],av[6],av[7],av[8],av[9]);break;
            }
        }catch (Exception e){
            return "Exception: " + e.getMessage()+"\n"+android.util.Log.getStackTraceString(e);
        }
        if(result == null){
            return "";
        }
        var rText = result.toString();
        // check is primitive type
        if(isWrapperType(result)){
            return rText;
        }
        if(WebNativeViewPlugin.instance.viewManager.objectList.contains(result)){
            return "ObjectID:"+WebNativeViewPlugin.instance.viewManager.objectList.indexOf(result);
        }else{
            var id = WebNativeViewPlugin.instance.viewManager.statObject(result);
            return "ObjectID:"+id;
        }
    }
    public static JSArray serializeMethodList(ArrayList<PViewManager.MethodInfo> mets){
        var array = new JSArray();
        for(var met : mets){
            array.put(met.serialize());
        }
        return array;
    }
    public static JSArray serializePropertyList(ArrayList<PViewManager.PropertyInfo> props){
        var array = new JSArray();
        for(var prop : props){
            array.put(prop.serialize());
        }
        return array;
    }
    public static JSObject serializeViewInfo(PViewManager.ViewPass viewInfo){
        var obj = new JSObject();
        obj.put("viewId",viewInfo.viewId);
        obj.put("properties", serializePropertyList(viewInfo.propertyInfos));
        obj.put("methods", serializeMethodList(viewInfo.methodInfos));
        return obj;
    }
    public static JSObject serializeObjectInfo(PViewManager.ObjectPass viewInfo){
        var obj = new JSObject();
        obj.put("viewId",viewInfo.viewId);
        obj.put("properties", serializePropertyList(viewInfo.propertyInfos));
        obj.put("methods", serializeMethodList(viewInfo.methodInfos));
        return obj;
    }
    public static JSObject getError(String message){
        var obj = new JSObject();
        obj.put("error", message);
        return obj;
    }
    public static JSObject getError(int message){
        var obj = new JSObject();
        obj.put("error", message);
        return obj;
    }
    public static JSObject getReturnNum(int message){
        var obj = new JSObject();
        obj.put("return", message);
        return obj;
    }
    public static JSObject getReturnString(String message){
        var obj = new JSObject();
        obj.put("return", message);
        return obj;
    }
    public static int[] getBytesFromBitmap(Bitmap bm){
        var width = bm.getWidth();
        var height = bm.getHeight();
        var pixels = new int[bm.getWidth() * bm.getHeight()];
        bm.getPixels(pixels, 0, width, 0, 0, width, height);
        return pixels;
    }
    public static String joinIntArr(int[] arr){
        return Arrays.toString(arr);
    }
}
