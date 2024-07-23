import { Plugin, PluginListenerHandle } from '@capacitor/core';

export interface WebNativeViewPlugin extends Plugin {
  invoke(options: { viewId: number, methodId: number, params: string[] }): Promise<any>;
  invokeObject(options: { viewId: number, methodId: number, params: string[] }): Promise<any>;
  setSize(options: { viewId: number, width: number, height: number }): Promise<any>;
  dispose(options: { viewId: number }): Promise<any>;
  update(options: { viewId: number }): Promise<any>;
  getViewData(options: { viewId: number }): Promise<any>;
  createView(options: { name: string }): Promise<any>;
  createObject(options: { name: string }): Promise<any>;
  lock(options: { viewId: number, left: number, top: number, width:number, height:number }): Promise<any>;
  unlock(options: {}): Promise<any>;
  addListener(eventName: 'invalidate', listenerFunc: (info: any) => void): Promise<PluginListenerHandle> & PluginListenerHandle;
  createHandler(options: { name: string, targetMethod: string, eventName: string }): Promise<any>;
  awaitEvent(options: { viewId: number, methodId: number}): Promise<any>;
  bindEventToMethod(options: { objId: number, methodId: number, eventName: string }): Promise<any>;
}
export class AndroidMethod {
  constructor(public context: WebNativeViewPlugin, public methodId: number) {}
  public invoke(obj: AndroidView, args: string[]): any {
    return this.context.invoke({ viewId: obj.viewId, methodId: this.methodId, params: args });
  }
}
export class AndroidView extends Object {
  public canvas: HTMLCanvasElement;
  public context2d: CanvasRenderingContext2D;
  constructor(public viewId: number, public context: WebNativeViewPlugin) {
    super();
    this.canvas = document.createElement('canvas');
    this.context2d = this.canvas.getContext('2d') as CanvasRenderingContext2D;
    context.addListener('invalidate', function(info: any) {
      console.log(info);
    });
  }
  methodList: AndroidMethod[] = [];
  loadMethodList(arr: any[]){
    for (let i = 0; i < arr.length; i++) {
      this.methodList.push(new AndroidMethod(this.context, i));
      var ref = this;
      this.methods[arr[i].name] = function() {
        return ref.methodList[i].invoke(ref, Array.from(arguments));
      }
    }
  }
  public methods : { [key: string]: any } = {}
  updateSize(){
    this.context.setSize({ viewId: this.viewId, width: this.canvas.width, height: this.canvas.height });
  }
  draw = () => {
    this.context.getViewData({ viewId: this.viewId }).then((data) => {
      var imgData = new Uint8ClampedArray(data.data);
      var imageData = new ImageData(imgData, data.width, data.height);
      this.context2d.putImageData(imageData, 0, 0);
    });
  }
}

export async function createView(p:WebNativeViewPlugin,name:string){
  var obj = await p.createView({ name: name });
  var view = new AndroidView(obj.viewId, p);
  view.loadMethodList(obj.methods);
  console.log(obj);
  return view;
}