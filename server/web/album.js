'use strict';
const $=id=>document.getElementById(id),key=location.pathname.split('/').pop(),selected=new Set();
let photos=[],index=0,loading=false;
const message=t=>{$('message').textContent=t;};
async function load(){
 if(loading||document.hidden)return;loading=true;
 try{const r=await fetch('/api/albums/'+key,{cache:'no-store'});if(r.status===401){$('unlock').hidden=false;$('count').textContent='Album dengan PIN';return;}
 if(!r.ok)throw new Error(r.status===404?'Album tidak ditemukan. Periksa kembali tautan QR.':'Album belum dapat dimuat. Coba perbarui.');
 const data=await r.json();$('unlock').hidden=true;$('title').textContent=data.title;document.title=data.title+' • Alpi';$('description').textContent=data.description||'Kenangan acara Anda, siap disimpan.';
 const changed=JSON.stringify(data.photos)!==JSON.stringify(photos);photos=data.photos;
 const ids=new Set(photos.map(p=>p.id));for(const id of selected)if(!ids.has(id))selected.delete(id);
 $('count').textContent=photos.length+' foto · Diperbarui '+new Date().toLocaleTimeString('id-ID',{hour:'2-digit',minute:'2-digit'});$('empty').hidden=photos.length>0;
 if(changed){render();if($('viewer').open){index=Math.min(index,photos.length-1);if(index<0)$('viewer').close();else show(index);}}message('');selection();
 }catch(e){message(e.message);}finally{loading=false;}
}
function render(){const grid=$('grid');grid.replaceChildren();photos.forEach((p,i)=>{const card=document.createElement('article');card.className='photo';const b=document.createElement('button');b.setAttribute('aria-label','Lihat foto '+(i+1));const im=document.createElement('img');im.loading='lazy';im.src=p.thumbnail;im.alt='Foto acara '+(i+1);b.append(im);b.onclick=()=>{show(i);$('viewer').showModal();};const label=document.createElement('label');label.className='pick';const cb=document.createElement('input');cb.type='checkbox';cb.checked=selected.has(p.id);cb.setAttribute('aria-label','Pilih foto '+(i+1));cb.onchange=()=>{cb.checked?selected.add(p.id):selected.delete(p.id);selection();};label.append(cb);card.append(b,label);grid.append(card);});}
function selection(){$('selection').hidden=!selected.size;$('selected-count').textContent=selected.size+' dipilih';}
function show(i){if(!photos.length)return;index=(i+photos.length)%photos.length;const p=photos[index];$('large').src=p.url;$('position').textContent=(index+1)+' / '+photos.length;$('download').href=p.url+'?download=true';}
$('close').onclick=()=>$('viewer').close();$('previous').onclick=()=>show(index-1);$('next').onclick=()=>show(index+1);$('viewer').addEventListener('keydown',e=>{if(e.key==='ArrowLeft')show(index-1);if(e.key==='ArrowRight')show(index+1);});
$('refresh').onclick=load;$('clear').onclick=()=>{selected.clear();render();selection();};
$('unlock').onsubmit=async e=>{e.preventDefault();const r=await fetch('/api/albums/'+key+'/unlock',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({pin:$('pin').value})});if(r.ok){$('pin').value='';await load();}else message(r.status===429?'Terlalu banyak percobaan. Tunggu 5 menit.':'PIN tidak sesuai. Silakan coba lagi.');};
$('share').onclick=async()=>{try{if(navigator.share)await navigator.share({title:document.title,url:location.href});else{await navigator.clipboard.writeText(location.href);message('Tautan album disalin.');}}catch(e){if(e.name!=='AbortError')message('Salin tautan dari bilah alamat browser.');}};
$('download-selected').onclick=async()=>{const button=$('download-selected');button.disabled=true;try{for(const p of photos.filter(p=>selected.has(p.id))){const r=await fetch(p.url+'?download=true');if(!r.ok)throw new Error('Unduhan gagal. Buka foto untuk mencoba satu per satu.');const url=URL.createObjectURL(await r.blob()),a=document.createElement('a');a.href=url;a.download='Alpi-'+p.id.slice(0,8)+'.jpg';document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),60000);await new Promise(resolve=>setTimeout(resolve,400));}message('Unduhan dikirim ke browser. Izinkan unduhan beberapa berkas jika diminta.');}catch(e){message(e.message);}finally{button.disabled=false;}};
let touchX=0;$('large').addEventListener('touchstart',e=>{if(e.touches.length===1)touchX=e.touches[0].clientX;},{passive:true});$('large').addEventListener('touchend',e=>{const diff=e.changedTouches[0].clientX-touchX;if(Math.abs(diff)>70)show(index+(diff<0?1:-1));},{passive:true});
document.addEventListener('visibilitychange',()=>{if(!document.hidden)load();});setInterval(load,12000);load();
