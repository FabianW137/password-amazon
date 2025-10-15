async function post(url, body){
  const res = await fetch(url, { method:'POST', headers:{'Content-Type':'application/json'}, body: body ? JSON.stringify(body) : null });
  if(!res.ok) throw new Error('HTTP '+res.status);
  return await res.json();
}
btnPin.onclick = async ()=>{ try{ await post('/api/pin',{pin: pin.value}); alert('PIN gesetzt'); }catch(e){ alert(e.message);} };
btnChallenge.onclick = async ()=>{ try{ const d=await post('/api/challenge'); challengeOut.textContent = `Code: ${d.code}\nGültig bis: ${new Date(d.expiresAt).toLocaleString()}`; codeIn.value=d.code; pinIn.value=pin.value; }catch(e){ alert(e.message);} };
btnPairing.onclick = async ()=>{ try{ const d=await post('/api/pairing'); pairingOut.textContent = `Pairing-Code: ${d.pairingCode}`; pairingIn.value=d.pairingCode; }catch(e){ alert(e.message);} };
btnLink.onclick = async ()=>{ try{ const d=await post('/api/link',{pairingCode: pairingIn.value, alexaUserId: alexaUser.value}); alert(d.ok?'Verknüpft':'Fehlgeschlagen'); }catch(e){ alert(e.message);} };
btnVerify.onclick = async ()=>{ try{ const d=await post('/api/verify',{code: codeIn.value, pin: pinIn.value, alexaUserId: alexaUser2.value, deviceId:'demo'}); verifyOut.textContent = d.success? 'OK' : ('Fehler: '+d.message); }catch(e){ alert(e.message);} };
