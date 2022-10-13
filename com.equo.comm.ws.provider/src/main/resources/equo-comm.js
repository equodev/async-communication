var EquoCommService;(()=>{"use strict";var e={d:(o,n)=>{for(var t in n)e.o(n,t)&&!e.o(o,t)&&Object.defineProperty(o,t,{enumerable:!0,get:n[t]})},o:(e,o)=>Object.prototype.hasOwnProperty.call(e,o)},o={};(()=>{var n,t;e.d(o,{EquoCommService:()=>c}),function(e){const o=window;function n(e){o[e.id]||(o[e.id]=e.service)}e.get=function(e,t){const r=o[e];if(!r&&null!=t){const o=t();if(!o)throw new Error(`${e} couldn't be created`);return n(o),o.service}if(r)return r;throw new Error(`${e} has not been installed`)},e.install=n}(n||(n={})),function(e){const o=[];for(let e=0;e<256;e++)o[e]=(e<16?"0":"")+e.toString(16);e.getUuid=function(){const e=4294967296*Math.random()>>>0,n=4294967296*Math.random()>>>0,t=4294967296*Math.random()>>>0,r=4294967296*Math.random()>>>0;return o[255&e]+o[e>>8&255]+o[e>>16&255]+o[e>>24&255]+"-"+o[255&n]+o[n>>8&255]+"-"+o[n>>16&15|64]+o[n>>24&255]+"-"+o[63&t|128]+o[t>>8&255]+"-"+o[t>>16&255]+o[t>>24&255]+o[255&r]+o[r>>8&255]+o[r>>16&255]+o[r>>24&255]}}(t||(t={}));var r=function(e,o,n,t){return new(n||(n=Promise))((function(r,i){function s(e){try{a(t.next(e))}catch(e){i(e)}}function c(e){try{a(t.throw(e))}catch(e){i(e)}}function a(e){var o;e.done?r(e.value):(o=e.value,o instanceof n?o:new n((function(e){e(o)}))).then(s,c)}a((t=t.apply(e,o||[])).next())}))};class i{constructor(e){this.userEventCallbacks=new Map,this.ws=this.getWebSocketIfExists(e),void 0===this.ws&&(window.equoReceiveMessage=e=>{this.receiveMessage(e)})}getWebSocketIfExists(e){if(void 0===e)return;if(void 0!==this.ws&&this.ws.readyState!==WebSocket.CLOSED)return this.ws;const o=new WebSocket(`ws://127.0.0.1:${e}`);return o.onopen=e=>{},o.onclose=()=>{},o.onmessage=e=>{this.receiveMessage(e.data)},o}receiveMessage(e){var o;const n=this.processMessage(e);if(n){const e=n.actionId;if(this.userEventCallbacks.has(e)){const t=this.userEventCallbacks.get(n.actionId);(null===(o=null==t?void 0:t.args)||void 0===o?void 0:o.once)&&this.userEventCallbacks.delete(e),void 0===n.error?void 0!==n.callbackId?Promise.resolve((()=>r(this,void 0,void 0,(function*(){return null==t?void 0:t.onSuccess(n.payload)})))()).then((e=>{this.sendToJava({actionId:n.callbackId,payload:e})})).catch((e=>{if("string"==typeof e)this.sendToJava({actionId:n.callbackId,error:e});else if(void 0!==e){const o=JSON.stringify(e);this.sendToJava({actionId:n.callbackId,error:o})}})):Promise.resolve((()=>r(this,void 0,void 0,(function*(){return null==t?void 0:t.onSuccess(n.payload)})))()).catch((e=>{console.error(e)})):void 0!==n.error&&(null==t?void 0:t.onError)&&Promise.resolve((()=>r(this,void 0,void 0,(function*(){return t.onError(n.error)})))()).catch((e=>{console.error(e)}))}}}processMessage(e){if(void 0===e)return null;try{return JSON.parse(e)}catch(e){return console.error(e),null}}sendToJava(e,o){var n;const t=JSON.stringify({actionId:e.actionId,payload:e.payload,error:e.error,callbackId:null==o?void 0:o.id});void 0!==window.equoSend?window.equoSend({request:t,onSuccess:e=>{let n;try{n=JSON.parse(e)}catch(o){n=e}null==o||o.onSuccess(n)},onFailure:(e,n)=>{-1===e&&"Unexpected call to CefQueryCallback_N::finalize()"===n||void 0!==(null==o?void 0:o.onError)&&o.onError({code:e,message:n})},persistent:!(null===(n=null==o?void 0:o.args)||void 0===n?void 0:n.once)}):void 0!==this.ws&&this.waitForCommConnection((()=>{var e;null===(e=this.ws)||void 0===e||e.send(t)}))}waitForCommConnection(e){setTimeout((()=>{void 0!==this.ws?this.ws.readyState===WebSocket.OPEN?e():this.waitForCommConnection(e):void 0===window.equoSend?this.waitForCommConnection(e):e()}),5)}send(e,o){return r(this,void 0,void 0,(function*(){return yield new Promise(((n,r)=>{const i={actionId:e,payload:o},s={onSuccess:n,onError:r,args:{once:!0}};void 0!==this.ws&&(s.id=t.getUuid(),this.on(s.id,n,r,s.args)),this.sendToJava(i,s)}))}))}on(e,o,n,t){const r={onSuccess:o,onError:n,args:t};this.userEventCallbacks.set(e,r)}remove(e){this.userEventCallbacks.delete(e)}}const s="equo-comm",c=n.get(s,(function(){let e;if(void 0===window.equoSend){const o=new URLSearchParams(window.location.search).get("equocommport");e=null===o?void 0:+o}return{id:s,service:new i(e)}}));window.addEventListener("load",(()=>{const e=t.getUuid();c.send("__equo_init",e),window.addEventListener("beforeunload",(()=>{c.send("__equo_uninit",e)}))}))})(),EquoCommService=o.EquoCommService})();