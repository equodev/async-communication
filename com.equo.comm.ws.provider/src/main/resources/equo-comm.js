var EquoCommService;(()=>{"use strict";var e={d:(o,n)=>{for(var t in n)e.o(n,t)&&!e.o(o,t)&&Object.defineProperty(o,t,{enumerable:!0,get:n[t]})},o:(e,o)=>Object.prototype.hasOwnProperty.call(e,o)},o={};(()=>{var n,t;e.d(o,{EquoCommService:()=>a}),function(e){const o=window;function n(e){o[e.id]||(o[e.id]=e.service)}e.get=function(e,t){const i=o[e];if(!i&&null!=t){const o=t();if(!o)throw new Error(`${e} couldn't be created`);return n(o),o.service}if(i)return i;throw new Error(`${e} has not been installed`)},e.install=n}(n||(n={})),function(e){const o=[];for(let e=0;e<256;e++)o[e]=(e<16?"0":"")+e.toString(16);e.getUuid=function(){const e=4294967296*Math.random()>>>0,n=4294967296*Math.random()>>>0,t=4294967296*Math.random()>>>0,i=4294967296*Math.random()>>>0;return o[255&e]+o[e>>8&255]+o[e>>16&255]+o[e>>24&255]+"-"+o[255&n]+o[n>>8&255]+"-"+o[n>>16&15|64]+o[n>>24&255]+"-"+o[63&t|128]+o[t>>8&255]+"-"+o[t>>16&255]+o[t>>24&255]+o[255&i]+o[i>>8&255]+o[i>>16&255]+o[i>>24&255]}}(t||(t={}));var i=function(e,o,n,t){return new(n||(n=Promise))((function(i,s){function r(e){try{c(t.next(e))}catch(e){s(e)}}function a(e){try{c(t.throw(e))}catch(e){s(e)}}function c(e){var o;e.done?i(e.value):(o=e.value,o instanceof n?o:new n((function(e){e(o)}))).then(r,a)}c((t=t.apply(e,o||[])).next())}))};class s{constructor(e,o){this.userEventCallbacks=new Map,this.ws=this.getWebSocketIfExists(e,o),void 0===this.ws&&(window.equoReceiveMessage=e=>{this.receiveMessage(e)})}getWebSocketIfExists(e,o){if(void 0===e||void 0===o)return;if(void 0!==this.ws&&this.ws.readyState!==WebSocket.CLOSED)return this.ws;const n=new WebSocket(`ws://${e}:${o}`);return n.onopen=e=>{},n.onclose=()=>{},n.onmessage=e=>{this.receiveMessage(e.data)},n}receiveMessage(e){var o;const n=this.processMessage(e);if(n){const e=n.actionId;if(this.userEventCallbacks.has(e)){const t=this.userEventCallbacks.get(n.actionId);(null===(o=null==t?void 0:t.args)||void 0===o?void 0:o.once)&&this.userEventCallbacks.delete(e),void 0===n.error?void 0!==n.callbackId?Promise.resolve((()=>i(this,void 0,void 0,(function*(){return null==t?void 0:t.onSuccess(n.payload)})))()).then((e=>{this.sendToJava({actionId:n.callbackId,payload:e})})).catch((e=>{const o={code:-1,message:""};"string"==typeof e?(o.message=e,this.sendToJava({actionId:n.callbackId,payload:o,error:"1"})):void 0!==e&&("number"==typeof e.code&&(o.code=e.code),o.message=JSON.stringify(e),this.sendToJava({actionId:n.callbackId,payload:o,error:"1"}))})):Promise.resolve((()=>i(this,void 0,void 0,(function*(){return null==t?void 0:t.onSuccess(n.payload)})))()).catch((e=>{console.error(e)})):void 0!==n.error&&(null==t?void 0:t.onError)&&Promise.resolve((()=>i(this,void 0,void 0,(function*(){t.onError(n.error)})))()).catch((e=>{console.error(e)}))}else if(void 0!==n.callbackId){const e={code:255,message:"An event handler does not exist for the user event '"+n.actionId+"'"};this.sendToJava({actionId:n.callbackId,payload:e,error:"1"})}}}processMessage(e){if(void 0===e)return null;if("object"==typeof e)return e;try{return JSON.parse(e)}catch(e){return console.error(e),null}}sendToJava(e,o,n){var t;const i=JSON.stringify({actionId:e.actionId,payload:e.payload,error:e.error,callbackId:null==o?void 0:o.id});if(void 0!==window.equoSend){const e=void 0!==(null==n?void 0:n.sequential)?"&-"+i:i;window.equoSend({request:e,onSuccess:e=>{let n;try{n=JSON.parse(e)}catch(o){n=e}null==o||o.onSuccess(n)},onFailure:(e,n)=>{-1===e&&"Unexpected call to CefQueryCallback_N::finalize()"===n||void 0!==(null==o?void 0:o.onError)&&o.onError({code:e,message:n})},persistent:!(null===(t=null==o?void 0:o.args)||void 0===t?void 0:t.once)})}else void 0!==this.ws&&this.waitForCommConnection((()=>{var e;null===(e=this.ws)||void 0===e||e.send(i)}))}waitForCommConnection(e){setTimeout((()=>{void 0!==this.ws?this.ws.readyState===WebSocket.OPEN?e():this.waitForCommConnection(e):void 0===window.equoSend?this.waitForCommConnection(e):e()}),5)}send(e,o,n){return i(this,void 0,void 0,(function*(){return yield new Promise(((i,s)=>{const r={actionId:e,payload:o},a={onSuccess:i,onError:s,args:{once:!0}};void 0!==this.ws&&(a.id=t.getUuid(),this.on(a.id,i,s,a.args)),this.sendToJava(r,a,n)}))}))}on(e,o,n,t){const i={onSuccess:o,onError:n,args:t};this.userEventCallbacks.set(e,i)}remove(e){this.userEventCallbacks.delete(e)}}const r="equo-comm",a=n.get(r,(function(){var e;let o,n=location.host;if(void 0===window.equoSend){const t=new URLSearchParams(window.location.search);n=null!==(e=t.get("equoCommHost"))&&void 0!==e?e:n;const i=t.get("equoCommPort");o=null===i?void 0:+i}return{id:r,service:new s(n,o)}}));window.addEventListener("load",(()=>{const e=t.getUuid();a.send("__equo_init",e,{sequential:!0}).catch((()=>{})),window.addEventListener("beforeunload",(()=>{a.send("__equo_uninit",e,{sequential:!0}).catch((()=>{}))}))}))})(),EquoCommService=o.EquoCommService})();