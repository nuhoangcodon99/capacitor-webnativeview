package com.sangtacviet.capacitorwebnative;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.JavascriptInterface;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.getcapacitor.Bridge;
import com.getcapacitor.JSObject;
import com.getcapacitor.JSArray;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONException;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@CapacitorPlugin(name = "WebNativeView")
public class WebNativeViewPlugin extends Plugin {

    private WebNativeView implementation = new WebNativeView();
    public PViewManager viewManager;
    public HandlerBuilder handlerBuilder = new HandlerBuilder();
    private Bridge bridge;
    @PluginMethod
    public void createView(PluginCall call) {
        String className = call.getString("name");
        bridge.getActivity().runOnUiThread(() -> {
            int viewId = viewManager.createView(className);
            if(viewId < 0){
                call.resolve(Util.getError(viewId));
                return;
            }
            var viewInfo = viewManager.viewInfoList.get(viewId);
            call.resolve(Util.serializeViewInfo(viewInfo));
        });
    }
    @PluginMethod
    public void createObject(PluginCall call) {
        String className = call.getString("name");
        bridge.getActivity().runOnUiThread(() -> {
            int viewId = viewManager.createObject(className);
            if(viewId < 0){
                call.resolve(Util.getError(viewId));
                return;
            }
            var viewInfo = viewManager.objectInfoList.get(viewId);
            call.resolve(Util.serializeObjectInfo(viewInfo));
        });
    }
    @PluginMethod
    public void invoke(PluginCall call) throws JSONException {
        int viewId = call.getInt("viewId",999999);
        if(viewId >= viewManager.viewInfoList.size()){
            call.resolve(Util.getError("View not found"));
            return;
        }
        var viewInfo = viewManager.viewInfoList.get(viewId);
        int methodId = call.getInt("methodId");
        var params = call.getArray("params");
        var paramsPass = new ArrayList<String>();
        for(var i = 0;i<params.length();i++){
            paramsPass.add(params.getString(i));
        }
        bridge.getActivity().runOnUiThread(() -> {
            var returnCode = viewManager.invokeMethod(viewId,methodId,paramsPass);
            if(returnCode != null && returnCode.contains("ObjectID:")){
                var viewId2 = Integer.parseInt(returnCode.split(":")[1]);
                var viewInfo2 = viewManager.objectInfoList.get(viewId2);
                call.resolve(Util.serializeObjectInfo(viewInfo2));
                return;
            }
            call.resolve(Util.getReturnString(returnCode));
        });
    }
    @PluginMethod
    public void invokeObject(PluginCall call) throws JSONException {
        int viewId = call.getInt("viewId",999999);
        if(viewId >= viewManager.objectInfoList.size()){
            call.resolve(Util.getError("Object not found"));
            return;
        }
        var viewInfo = viewManager.objectInfoList.get(viewId);
        int methodId = call.getInt("methodId");
        var params = call.getArray("params");
        var paramsPass = new ArrayList<String>();
        for(var i = 0;i<params.length();i++){
            paramsPass.add(params.getString(i));
        }
        bridge.getActivity().runOnUiThread(() -> {
            var returnCode = viewManager.invokeObjectMethod(viewId,methodId,paramsPass);
            if(returnCode != null && returnCode.contains("ObjectID:")){
                var objId = Integer.parseInt(returnCode.split(":")[1]);
                var objInfo = viewManager.objectInfoList.get(objId);
                call.resolve(Util.serializeObjectInfo(objInfo));
                return;
            }
            call.resolve(Util.getReturnString(returnCode));
        });
    }
    @PluginMethod
    public void getProperty(PluginCall call) throws JSONException {
        int viewId = call.getInt("viewId",999999);
        if(viewId >= viewManager.viewInfoList.size()){
            call.resolve(Util.getError("View not found"));
            return;
        }
        var viewInfo = viewManager.viewInfoList.get(viewId);
        int propId = call.getInt("propId");
        var propInfo = viewInfo.propertyInfos.get(propId);
        var view = viewManager.viewList.get(viewId);
        Object ret= null;
        try {
            ret = propInfo.refer.get(view);
        } catch (IllegalAccessException e) {
            call.resolve(Util.getError(e.toString()));
            return;
        }
        call.resolve(Util.getReturnString(ret.toString()));
    }
    public BufferInterface bufferInterface;
    public class BufferInterface {
        public int[] buffer;
        @JavascriptInterface
        public int[] getBuffer(){
            return buffer;
        }
    }
    @PluginMethod
    public void getViewData(PluginCall call){
        int viewId = call.getInt("viewId",999999);
        if(viewId >= viewManager.viewInfoList.size()){
            call.resolve(Util.getError("View not found"));
            return;
        }
        var view = viewManager.viewList.get(viewId);
        var obj = new JSObject();
        mainActivity.runOnUiThread(()->{
            var lp = view.getLayoutParams();
            Bitmap bm = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                bm = Bitmap.createBitmap(lp.width,lp.height, Bitmap.Config.RGBA_F16);
            }
            var canvas = new Canvas(bm);
            view.draw(canvas);
            if(bm == null){
                call.resolve(Util.getError("View not initialized"));
                view.invalidate();
                return;
            }
            var w = bm.getWidth();
            var h = bm.getHeight();
            var data = Util.getBytesFromBitmap(bm);
            obj.put("data", Util.joinIntArr(data));
            obj.put("width", w);
            obj.put("height", h);
            call.resolve(obj);
        });
    }
    @PluginMethod
    public void awaitEvent(PluginCall call){
        int viewId = call.getInt("viewId",999999);
        if(viewId >= viewManager.viewInfoList.size()){
            call.resolve(Util.getError("View not found"));
            return;
        }
        var view = viewManager.viewList.get(viewId);
        var obj = new JSObject();
        var methodId = call.getInt("methodId");
        mainActivity.runOnUiThread(()->{
            // var method = viewManager.viewInfoList.get(viewId).methodInfos.get(methodId);
            // var eventListener = new Runnable(){
            //     @Override
            //     public void run() {
            //         var retStr = Util.getReturnString("");
            //         call.resolve(retStr);
            //     }
            // };
            // method.invoke(view,eventListener);
            var method = viewManager.viewInfoList.get(viewId).methodInfos.get(methodId);
            // use reflection proxy
            var clazz = method.refer.getParameterTypes()[0];

            var proxy = Proxy.newProxyInstance(
                view.getClass().getClassLoader(),
                new Class[]{clazz},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        var retStr = Util.getReturnString("");
                        call.resolve(retStr);
                        return null;
                    }
                });
            try {
                method.refer.invoke(view,clazz.cast(proxy));
            } catch (IllegalAccessException e) {
                call.resolve(Util.getError(e.toString()));
                return;
            } catch (InvocationTargetException e) {
                call.resolve(Util.getError(e.toString()));
                return;
            }
        });
    }
    @PluginMethod
    public void createHandler(PluginCall call){
        var className = call.getString("name");
        var targetMethod = call.getString("targetMethod");
        var eventName = call.getString("eventName");
        Class clazz = null;
        try {
            clazz = viewManager.classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            call.resolve(Util.getError(e.toString()));
            return;
        }
        Object proxy = null;
        try {
            proxy = handlerBuilder.createEventListener(clazz,eventName,targetMethod,bridge);
            if(proxy instanceof Exception){
                call.resolve(Util.getError(proxy.toString() +android.util.Log.getStackTraceString((Exception)proxy)));
                return;
            }
            var objid = viewManager.statObject(proxy);
            var objInfo = viewManager.objectInfoList.get(objid);
            call.resolve(Util.serializeObjectInfo(objInfo));
        } catch (Exception e) {
            call.resolve(Util.getError(e.toString()+android.util.Log.getStackTraceString(e)));
            return;
        }
    }

    @PluginMethod
    public void bindEventToMethod(PluginCall call){
        var objId = call.getInt("objId");
        if(objId > viewManager.objectList.size()){
            call.resolve(Util.getError("Object not found"));
            return;
        }
        var objInfo = viewManager.objectInfoList.get(objId);
        var obj = viewManager.objectList.get(objId);
        var eventName = call.getString("eventName");
        var methodId = call.getInt("methodId");
        var method = objInfo.methodInfos.get(methodId).refer;
        // get first parameter class
        var paramTypes = method.getParameterTypes();
        var paramType = paramTypes[0];
        Class clazz = null;
        try {
            clazz = viewManager.classLoader.loadClass(paramType.getName());
        } catch (ClassNotFoundException e) {
            call.resolve(Util.getError(e.toString()));
            return;
        }
        var proxy = Proxy.newProxyInstance(
            viewManager.classLoader,
            new Class[]{clazz},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    var l = args.length;
                    var jsonArgs = new JSArray();
                    for(int i = 0;i < l;i++){
                        if(args[i] != null){
                            if(Util.isWrapperType(args[i])){
                                jsonArgs.put(args[i]);
                            }else{
                                var objId = viewManager.statObject(args[i]);
                                jsonArgs.put(Util.serializeObjectInfo(viewManager.objectInfoList.get(objId)));
                            }
                        }
                    }
                    //bridge.triggerJSEvent(eventName,new JSObject().put("name",obj.getClass().getName()).put("method",method.getName()).put("args",jsonArgs));
                    bridge.triggerJSEvent(eventName, "document", "{\"name\":\""+obj.getClass().getName()+"\",\"method\":\""+method.getName()+"\"}");
                    return null;
                }
            });
        try {
            method.invoke(obj,clazz.cast(proxy));
        } catch (IllegalAccessException e) {
            call.resolve(Util.getError(e.toString()));
            return;
        } catch (InvocationTargetException e) {
            call.resolve(Util.getError(e.toString()));
            return;
        }
        call.resolve(Util.getReturnString(""));
    }

    @PluginMethod
    public void update(PluginCall call){
        int viewId = call.getInt("viewId",999999);
        if(viewId >= viewManager.viewInfoList.size()){
            call.resolve(Util.getError("View not found"));
            return;
        }
        var view = viewManager.viewList.get(viewId);
        view.invalidate();
        var obj = new JSObject();
        var bm = view.getDrawingCache();
        var w = bm.getWidth();
        var h = bm.getHeight();
        obj.put("data", Util.getBytesFromBitmap(bm));
        obj.put("width", w);
        obj.put("height", h);
        call.resolve(obj);
    }
    @PluginMethod
    public void lock(PluginCall call){
        int viewId = call.getInt("viewId",999999);
        if(viewId >= viewManager.viewInfoList.size()){
            call.resolve(Util.getError("View not found"));
            return;
        }
        var view = viewManager.viewList.get(viewId);
        var width = call.getInt("width");
        var height = call.getInt("height");
        var left = call.getInt("left");
        var top = call.getInt("top");
        try {
            mainActivity.runOnUiThread(() -> {
                view.getLayoutParams().width = width;
                view.getLayoutParams().height = height;
                //view.setLeft(left);
                ///view.setTop(top);
                var lp = (CoordinatorLayout.LayoutParams)view.getLayoutParams();
                lp.leftMargin = left;
                lp.topMargin = top;
                view.setVisibility(View.VISIBLE);
            });
        }catch (Exception e){
            call.resolve(Util.getError("View not initialized"));
            return;
        }
    }
    @PluginMethod
    public void unlock(PluginCall call){
        try {
            mainActivity.runOnUiThread(() -> {
                for(View v : viewManager.viewList){
                    v.setVisibility(View.GONE);
                }
            });
        }catch (Exception e){
            return;
        }
    }
    @PluginMethod
    public void addEventListener(PluginCall call){
        var evName = call.getString("event");
        int viewId = call.getInt("viewId",999999);
        if(viewId >= viewManager.viewInfoList.size()){
            call.resolve(Util.getError("View not found"));
            return;
        }
        var view = viewManager.viewList.get(viewId);
    }
    public void onUpdate(int viewId){
        bridge.triggerJSEvent("invalidate","document",""+viewId);
    }
    @PluginMethod
    public void dispose(PluginCall call){
        int viewId = call.getInt("viewId",999999);
        if(viewId >= viewManager.viewInfoList.size()){
            call.resolve(Util.getError("View not found"));
            return;
        }
        viewManager.viewList.remove(viewId);
        call.resolve(Util.getReturnString("success"));
    }
    public static WebNativeViewPlugin instance;
    public static AppCompatActivity mainActivity;
    public static ViewGroup holder;
    @Override
    public void load() {
        bridge = getBridge();
        viewManager = new PViewManager(ClassLoader.getSystemClassLoader(), getContext());
        instance = this;
        mainActivity = getActivity();
        holder = (ViewGroup)bridge.getWebView().getParent();
        bufferInterface = new BufferInterface();
        bridge.getWebView().addJavascriptInterface(bufferInterface,"ViewBuffer");
        handlerBuilder.setContext(bridge.getContext());
    }
    @PluginMethod
    public void setSize(PluginCall call){
        int viewId = call.getInt("viewId",999999);
        if(viewId >= viewManager.viewInfoList.size()){
            call.resolve(Util.getError("View not found"));
            return;
        }
        var view = viewManager.viewList.get(viewId);
        var width = call.getInt("width");
        var height = call.getInt("height");
        try {
            mainActivity.runOnUiThread(() -> {
                view.getLayoutParams().width = width;
                view.getLayoutParams().height = height;
            });
        }catch (Exception e){
            call.resolve(Util.getError("View not initialized"));
            return;
        }

        call.resolve(Util.getReturnString("success"));
    }

}
