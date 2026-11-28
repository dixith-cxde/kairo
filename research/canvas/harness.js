const fs=require('fs');
const s=fs.readFileSync('project/Main.dc.html','utf8');
const a=s.split("data-dc-script data-props='")[1]; const k=a.indexOf("'>");
const props=JSON.parse(a.slice(0,k)); const js=a.slice(k+2).split('</script>')[0];
global.requestAnimationFrame=f=>0; global.cancelAnimationFrame=()=>{}; global.performance={now:()=>0};
class DCLogic{constructor(){this.state={}} setState(p){this.state={...this.state,...p}}}
global.DCLogic=DCLogic;
eval(js+';global.Component=Component;');
let bad=0; const states=props.startState.options;
for(const st of states) for(const th of ['dark','light']){ try{ const c=new Component(); c.props={startState:st,theme:th}; c.state={}; c.renderVals(); }catch(e){bad++;console.log('ERR',st,th,e.stack.split('\n').slice(0,3).join(' | '))} }
console.log('states', states.length, 'errors', bad);
