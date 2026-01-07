import{c as l}from"./index.js";import{z as c,v as u,N as i,s as n,k as d,c as x,V as m,K as w,e as v,U as O,h as _}from"./main.js";const U={__name:"Card",props:{class:{type:null,required:!1}},setup(e){const r=e;return(t,s)=>(d(),c("div",{class:i(n(l)("rounded-xl border bg-card text-card-foreground shadow",r.class))},[u(t.$slots,"default")],2))}},G={__name:"CardContent",props:{class:{type:null,required:!1}},setup(e){const r=e;return(t,s)=>(d(),c("div",{class:i(n(l)("p-6 pt-0",r.class))},[u(t.$slots,"default")],2))}},I={__name:"CardDescription",props:{class:{type:null,required:!1}},setup(e){const r=e;return(t,s)=>(d(),c("p",{class:i(n(l)("text-sm text-muted-foreground",r.class))},[u(t.$slots,"default")],2))}},K={__name:"CardHeader",props:{class:{type:null,required:!1}},setup(e){const r=e;return(t,s)=>(d(),c("div",{class:i(n(l)("flex flex-col gap-y-1.5 p-6",r.class))},[u(t.$slots,"default")],2))}},S={__name:"CardTitle",props:{class:{type:null,required:!1}},setup(e){const r=e;return(t,s)=>(d(),c("h3",{class:i(n(l)("font-semibold leading-none tracking-tight",r.class))},[u(t.$slots,"default")],2))}};typeof WorkerGlobalScope<"u"&&globalThis instanceof WorkerGlobalScope;const T=e=>typeof e<"u";function b(e){return m(e)?w(new Proxy({},{get(r,t,s){return n(Reflect.get(e.value,t,s))},set(r,t,s){return m(e.value[t])&&!m(s)?e.value[t].value=s:e.value[t]=s,!0},deleteProperty(r,t){return Reflect.deleteProperty(e.value,t)},has(r,t){return Reflect.has(e.value,t)},ownKeys(){return Object.keys(e.value)},getOwnPropertyDescriptor(){return{enumerable:!0,configurable:!0}}})):w(e)}function q(e){return b(x(e))}function V(e,...r){const t=r.flat(),s=t[0];return q(()=>Object.fromEntries(typeof s=="function"?Object.entries(v(e)).filter(([a,p])=>!s(O(p),a)):Object.entries(v(e)).filter(a=>!t.includes(a[0]))))}/**
 * @license lucide-vue-next v0.552.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const y=e=>e.replace(/([a-z0-9])([A-Z])/g,"$1-$2").toLowerCase(),P=e=>e.replace(/^([A-Z])|[\s-_]+(\w)/g,(r,t,s)=>s?s.toUpperCase():t.toLowerCase()),A=e=>{const r=P(e);return r.charAt(0).toUpperCase()+r.slice(1)},B=(...e)=>e.filter((r,t,s)=>!!r&&r.trim()!==""&&s.indexOf(r)===t).join(" ").trim(),C=e=>e==="";/**
 * @license lucide-vue-next v0.552.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */var o={xmlns:"http://www.w3.org/2000/svg",width:24,height:24,viewBox:"0 0 24 24",fill:"none",stroke:"currentColor","stroke-width":2,"stroke-linecap":"round","stroke-linejoin":"round"};/**
 * @license lucide-vue-next v0.552.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const E=({name:e,iconNode:r,absoluteStrokeWidth:t,"absolute-stroke-width":s,strokeWidth:a,"stroke-width":p,size:f=o.width,color:k=o.stroke,...h},{slots:g})=>_("svg",{...o,...h,width:f,height:f,stroke:k,"stroke-width":C(t)||C(s)||t===!0||s===!0?Number(a||p||o["stroke-width"])*24/Number(f):a||p||o["stroke-width"],class:B("lucide",h.class,...e?[`lucide-${y(A(e))}-icon`,`lucide-${y(e)}`]:["lucide-icon"])},[...r.map($=>_(...$)),...g.default?[g.default()]:[]]);/**
 * @license lucide-vue-next v0.552.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const Z=(e,r)=>(t,{slots:s,attrs:a})=>_(E,{...a,...t,iconNode:r,name:e},s);export{K as _,S as a,I as b,Z as c,G as d,U as e,T as i,V as r};
