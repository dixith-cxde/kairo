import sys,re,json
src,out,props=sys.argv[1],sys.argv[2],sys.argv[3]; st=sys.argv[4] if len(sys.argv)>4 else "{}"
s=open(src).read()
helmet=s.split('<helmet>')[1].split('</helmet>')[0]
body=s.split('</helmet>')[1].split('</x-dc>')[0]
js=s.split('data-dc-script data-props=')[1].split("'>",1)[1].split('</script>')[0]
html='''<!doctype html><html><head><meta charset="utf-8">%s</head><body style="margin:0"><template id="t">%s</template><div id="root"></div>
<script>class DCLogic{constructor(){this.state={}} setState(p){this.state={...this.state,...p}}}
%s
const c=new Component(); c.props=%s; c.state=%s;
const v=c.renderVals();
function get(sc,path){return path.trim().split('.').reduce((o,k)=>o==null?o:o[k],sc)}
function sub(str,sc){return str.replace(/\\{\\{([^}]+)\\}\\}/g,(m,p)=>{p=p.trim(); if(p==='true')return true; if(p==='false')return false; const x=get(sc,p); return typeof x==='function'?'':(x==null?'':x)})}
function render(node,sc,parent){
 if(node.nodeType===3){parent.appendChild(document.createTextNode(sub(node.textContent,sc)));return}
 if(node.nodeType!==1)return;
 const tag=node.tagName.toLowerCase();
 if(tag==='sc-if'){const val=get(sc,node.getAttribute('value').replace(/[{}]/g,'')); if(val) node.childNodes.forEach(ch=>render(ch,sc,parent)); return}
 if(tag==='sc-for'){const list=get(sc,node.getAttribute('list').replace(/[{}]/g,''))||[]; const as=node.getAttribute('as'); list.forEach(it=>{const s2=Object.assign(Object.create(sc),{[as]:it}); node.childNodes.forEach(ch=>render(ch,s2,parent))}); return}
 const el=node.namespaceURI==='http://www.w3.org/2000/svg'?document.createElementNS(node.namespaceURI,node.tagName):document.createElement(node.tagName);
 for(const a of node.attributes){ if(/^on[A-Z]/.test(a.name)||/^on[a-z]/.test(a.name))continue; el.setAttribute(a.name,sub(a.value,sc)); }
 node.childNodes.forEach(ch=>render(ch,sc,el)); parent.appendChild(el);
}
document.getElementById('t').content.childNodes.forEach(ch=>render(ch,v,document.getElementById('root')));
</script></body></html>'''%(helmet,body,js,props,st)
open(out,'w').write(html)
