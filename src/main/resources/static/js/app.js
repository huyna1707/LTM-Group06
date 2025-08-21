// js/app.js

/* ------------ Helper ------------ */
const $ = (sel) => document.querySelector(sel);


/** === Toggle Dark/Light cho toàn trang (giữ nguyên các chức năng khác) === */
(function () {
    const html = document.documentElement;
    const btn  = document.getElementById("themeToggle");
    const icon = document.getElementById("themeIcon");
    const text = document.getElementById("themeText");

    const saved = localStorage.getItem("webchat-theme") || "light";

    apply(saved);

    btn?.addEventListener("click", () => {
        const next = html.classList.contains("dark") ? "light" : "dark";
        apply(next);
    });

    function apply(theme) {

        html.classList.toggle("dark", theme === "dark");
        localStorage.setItem("webchat-theme", theme);


        if (btn) {
            btn.className =
                "px-4 py-2 rounded-xl border shadow toolbar-float flex items-center gap-2 transition " +
                (theme === "dark"
                    ? "bg-gray-800 text-gray-100 border-gray-600 hover:bg-gray-700"
                    : "bg-slate-100 text-gray-700 border-gray-300 hover:bg-slate-200");
        }
        if (icon) icon.textContent = theme === "dark" ? "🌙" : "🌞";
        if (text) text.textContent = theme === "dark" ? "Dark" : "Light";
    }
})();




/* ================= Chat core ================= */
const messageInput   = $("#messageInput");
const sendButton     = $("#sendButton");
const chatMessages   = $("#chatMessages");
const typingIndicator= $("#typingIndicator");
const attachBtn    = $("#attachBtn");
const fileInput    = $("#fileInput");
function addMessage(text, isUser = true) {
    if (!chatMessages || !typingIndicator) return;

    const messageDiv = document.createElement("div");
    messageDiv.className = `flex items-start space-x-3 message-bubble ${isUser ? "justify-end" : ""}`;
    const time = new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });

    if (isUser) {
        messageDiv.innerHTML = `
      <div class="bg-gradient-to-r from-purple-500 to-purple-700 rounded-2xl rounded-tr-md px-4 py-3 max-w-xs lg:max-w-md">
        <p class="text-white">${text}</p>
        <span class="text-xs text-purple-100 mt-1 block">${time}</span>
      </div>
      <div class="w-8 h-8 bg-gradient-to-r from-purple-400 to-pink-500 rounded-full flex items-center justify-center flex-shrink-0">
        <span class="text-white text-sm">👤</span>
      </div>`;
    } else {
        messageDiv.innerHTML = `
      <div class="w-8 h-8 bg-gradient-to-r from-green-400 to-blue-500 rounded-full flex items-center justify-center flex-shrink-0">
        <span class="text-white text-sm">🤖</span>
      </div>
      <div class="bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-md px-4 py-3 max-w-xs lg:max-w-md">
        <p class="text-gray-800 dark:text-gray-200">${text}</p>
        <span class="text-xs text-gray-500 dark:text-gray-400 mt-1 block">${time}</span>
      </div>`;
    }

    chatMessages.insertBefore(messageDiv, typingIndicator);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function showTyping(){ typingIndicator?.classList.remove("hidden"); chatMessages && (chatMessages.scrollTop = chatMessages.scrollHeight); }
function hideTyping(){ typingIndicator?.classList.add("hidden"); }

function sendMessage(){
    if (!messageInput) return;
    const text = messageInput.value.trim();
    if (!text) return;
    addMessage(text, true);
    messageInput.value = "";

    showTyping();
    setTimeout(() => {
        hideTyping();
        const responses = [
            "Cảm ơn bạn đã sử dụng WebChat Pro! 😊",
            "Tin nhắn của bạn đã được nhận. Giao diện này có thiết kế responsive tuyệt vời! 🎨",
            "Tôi thích cách bạn tương tác với giao diện này! ✨",
            "WebChat Pro hỗ trợ chế độ sáng/tối và nhiều tính năng hiện đại! 🌟",
            "Hiệu ứng bo góc và màu sắc gradient thật chuyên nghiệp! 🎯",
        ];
        const randomResponse = responses[Math.floor(Math.random() * responses.length)];
        addMessage(randomResponse, false);
    }, 1200);
}

sendButton?.addEventListener("click", sendMessage);
messageInput?.addEventListener("keypress", (e) => { if (e.key === "Enter") sendMessage(); });
messageInput?.focus();

/* ================= Modal helpers ================= */
function openModal(id){ document.getElementById(id)?.classList.remove("hidden"); }
function closeModal(id){ document.getElementById(id)?.classList.add("hidden"); }
$("#addFriendBtn")?.addEventListener("click", () => openModal("addFriendModal"));
$("#createGroupBtn")?.addEventListener("click", () => openModal("createGroupModal"));

/* ================= Chat list helpers ================= */
function timeNowLabel(){ return new Date().toLocaleTimeString("vi-VN",{hour:"2-digit",minute:"2-digit"}); }
function getInitials(name){ if(!name) return "UU"; const p=name.trim().split(/\s+/).filter(Boolean); return p.length===1?p[0].slice(0,2).toUpperCase():(p[0][0]+p[p.length-1][0]).toUpperCase(); }
const gradients=["from-green-500 to-teal-500","from-blue-500 to-indigo-500","from-red-500 to-pink-500","from-purple-500 to-pink-500","from-emerald-500 to-cyan-500","from-amber-500 to-orange-600"];
function pickGradient(seed=0){ return gradients[seed % gradients.length]; }

function createChatItem({ name, preview="Tin nhắn mới", unread=true, power=1, gradientSeed=0 }){
    const initials = getInitials(name);
    const gradient = pickGradient(gradientSeed);
    const wrapper = document.createElement("div");
    wrapper.className = "chat-item p-3 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors";
    wrapper.innerHTML = `
    <div class="flex items-center space-x-3">
      <div class="relative">
        <div class="w-12 h-12 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center">
          <span class="text-white font-bold">${initials}</span>
        </div>
      </div>
      <div class="flex-1 min-w-0">
        <div class="flex items-center space-x-2">
          <h3 class="font-semibold text-gray-900 dark:text-white truncate">${name}</h3>
          <div class="flex items-center space-x-1">
            <span class="text-yellow-500">⚡</span>
            <span class="text-xs font-bold text-yellow-600 dark:text-yellow-400">${power}</span>
          </div>
        </div>
        <p class="text-sm text-gray-600 dark:text-gray-400 truncate">${preview}</p>
        <span class="text-xs text-gray-500 dark:text-gray-500">${timeNowLabel()}</span>
      </div>
      ${unread ? '<div class="w-2 h-2 bg-purple-500 rounded-full"></div>' : ''}
    </div>`;
    return wrapper;
}

const chatList = $("#chatList");
$("#confirmAddFriend")?.addEventListener("click", () => {
    const input = $("#friendInput");
    const value = (input?.value || "").trim(); if (!value) return;
    const node = createChatItem({ name:`Người dùng ${value}`, preview:"Vừa được thêm vào danh bạ", unread:true, power:1, gradientSeed:value.length });
    chatList?.prepend(node);
    closeModal("addFriendModal"); if (input) input.value = "";
});

$("#confirmCreateGroup")?.addEventListener("click", () => {
    const nameEl=$("#groupName"), membersEl=$("#groupMembers");
    const name=(nameEl?.value||"").trim(), members=(membersEl?.value||"").trim();
    if(!name||!members) return;
    const arr=members.split(",").map(s=>s.trim()).filter(Boolean);
    const preview=`Thành viên: ${arr.slice(0,3).join(", ")}${arr.length>3?"...":""}`;
    const node=createChatItem({ name, preview, unread:true, power:Math.max(1,Math.min(12,arr.length)), gradientSeed:name.length+arr.length });
    chatList?.prepend(node);
    closeModal("createGroupModal"); if(nameEl) nameEl.value=""; if(membersEl) membersEl.value="";
});

/* ================= Profile / Status ================= */
const profileNameEl=$("#profileName"), profileStatusEl=$("#profileStatus"), editProfileBtn=$("#editProfileBtn");
const editNameInput=$("#editName"), editStatusSelect=$("#editStatus"), editPersistChk=$("#editPersist");
const appStatusText=$("#appStatusText"), appStatusDot=$("#appStatusDot"), profileDot=$("#profileDot");

const STATUS_MAP={ active:{label:"Đang hoạt động",colorClass:"bg-green-500"}, busy:{label:"Đang bận",colorClass:"bg-yellow-500"}, offline:{label:"Tắt trạng thái hoạt động",colorClass:"bg-red-500"} };
const STATUS_KEYS=Object.keys(STATUS_MAP); const ALL_DOT_CLASSES=["bg-green-500","bg-yellow-500","bg-red-500"];

function labelToKey(label=""){ label=(label||"").toLowerCase().trim(); if(label.includes("bận"))return"busy"; if(label.includes("tắt"))return"offline"; return"active"; }
function updateDotsColor(dotEl,key){ if(!dotEl)return; dotEl.classList.remove(...ALL_DOT_CLASSES); dotEl.classList.add(STATUS_MAP[key].colorClass); }
function updateStatusUI(key){ const {label}=STATUS_MAP[key]||STATUS_MAP.active; if(profileStatusEl) profileStatusEl.textContent=label; if(appStatusText) appStatusText.textContent=label; updateDotsColor(profileDot,key); updateDotsColor(appStatusDot,key); }

(function initProfile(){
    const nameLS=localStorage.getItem("profileName");
    const keyLS =localStorage.getItem("profileStatusKey");
    const textLS=localStorage.getItem("profileStatus");
    if(nameLS&&profileNameEl) profileNameEl.textContent=nameLS;
    let initKey="active"; if(keyLS&&STATUS_MAP[keyLS]) initKey=keyLS; else if(textLS) initKey=labelToKey(textLS);
    updateStatusUI(initKey); if(editStatusSelect) editStatusSelect.value=initKey;
})();

/* ================= Avatar ================= */
const profileAvatarImg=$("#profileAvatarImg"), profileAvatarFallback=$("#profileAvatarFallback");
const editAvatarPreview=$("#editAvatarPreview"), editAvatarFallback=$("#editAvatarFallback"), editAvatarBtn=$("#editAvatarBtn"), editAvatarFile=$("#editAvatarFile"), removeAvatarBtn=$("#removeAvatarBtn");

function applyAvatar(dataUrl){
    if(profileAvatarImg&&profileAvatarFallback){
        if(dataUrl){ profileAvatarImg.src=dataUrl; profileAvatarImg.style.display="block"; profileAvatarImg.classList.add("w-12","h-12"); profileAvatarFallback.style.display="none"; }
        else { profileAvatarImg.removeAttribute("src"); profileAvatarImg.style.display="none"; profileAvatarFallback.style.display="flex"; }
    }
    if(editAvatarPreview&&editAvatarFallback){
        if(dataUrl){ editAvatarPreview.src=dataUrl; editAvatarPreview.style.display="block"; editAvatarFallback.style.display="none"; }
        else { editAvatarPreview.removeAttribute("src"); editAvatarPreview.style.display="none"; editAvatarFallback.style.display="flex"; }
    }
}
function fileToDataURL(file){ return new Promise((res,rej)=>{ const r=new FileReader(); r.onload=()=>res(r.result); r.onerror=rej; r.readAsDataURL(file); }); }
(function initAvatar(){ const saved=localStorage.getItem("profileAvatar")||""; applyAvatar(saved||null); })();

editProfileBtn?.addEventListener("click",()=>{
    const currentName=localStorage.getItem("profileName")||(profileNameEl?.textContent?.trim()||"Bạn");
    const currentKey =localStorage.getItem("profileStatusKey")||labelToKey(profileStatusEl?.textContent?.trim()||"");
    if(editNameInput) editNameInput.value=currentName;
    if(editStatusSelect) editStatusSelect.value=STATUS_KEYS.includes(currentKey)?currentKey:"active";
    applyAvatar(localStorage.getItem("profileAvatar")||null);
    if(editPersistChk){
        editPersistChk.checked=Boolean(localStorage.getItem("profileName")||localStorage.getItem("profileStatusKey")||localStorage.getItem("profileStatus"));
    }
    openModal("editProfileModal");
});

$("#saveProfileChanges")?.addEventListener("click",()=>{
    const newName=(editNameInput?.value.trim()||"Bạn");
    const key=editStatusSelect?.value||"active"; const safe=STATUS_KEYS.includes(key)?key:"active";
    if(profileNameEl) profileNameEl.textContent=newName; updateStatusUI(safe);
    if(editPersistChk?.checked){ localStorage.setItem("profileName",newName); localStorage.setItem("profileStatusKey",safe); localStorage.setItem("profileStatus",STATUS_MAP[safe].label); }
    else { localStorage.removeItem("profileName"); localStorage.removeItem("profileStatusKey"); localStorage.removeItem("profileStatus"); }
    closeModal("editProfileModal");
});

editAvatarBtn?.addEventListener("click",()=>editAvatarFile?.click());
editAvatarFile?.addEventListener("change", async (e)=>{
    const f=e.target.files?.[0]; if(!f) return;
    if (f.size > 5*1024*1024){ alert("Ảnh quá lớn. Vui lòng chọn ảnh < 5MB."); editAvatarFile.value=""; return; }
    try{ const dataUrl=await fileToDataURL(f); localStorage.setItem("profileAvatar",dataUrl); applyAvatar(dataUrl); }
    catch(err){ console.error(err); alert("Không đọc được file ảnh."); }
});
removeAvatarBtn?.addEventListener("click",()=>{ localStorage.removeItem("profileAvatar"); if(editAvatarFile) editAvatarFile.value=""; applyAvatar(null); });

/* ================= Chat Settings Drawer ================= */
const chatSettingsBtn=$("#chatSettingsBtn"), chatSettingsPanel=$("#chatSettingsPanel"), chatSettingsOverlay=$("#chatSettingsOverlay"), closeChatSettings=$("#closeChatSettings"), mainChatArea=$("#mainChatArea");
function openChatSettings(){ chatSettingsPanel?.classList.remove("translate-x-full"); chatSettingsOverlay?.classList.remove("hidden"); mainChatArea?.classList.add("mr-80"); }
function closeChatSettingsPanel(){ chatSettingsPanel?.classList.add("translate-x-full"); chatSettingsOverlay?.classList.add("hidden"); mainChatArea?.classList.remove("mr-80"); }
chatSettingsBtn?.addEventListener("click", openChatSettings);
closeChatSettings?.addEventListener("click", closeChatSettingsPanel);
chatSettingsOverlay?.addEventListener("click", closeChatSettingsPanel);
window.addEventListener("keydown",(e)=>{ if(e.key==="Escape") closeChatSettingsPanel(); });

/* ================= Wallpaper (vùng chat) ================= */
const wpPresetsEl=$("#wpPresets"), wpUrlInput=$("#wpUrl"), applyWpUrlBtn=$("#applyWpUrl"), resetWpBtn=$("#resetWp");
const WP_PRESETS={ none:{type:"preset",key:"none"}, "grad-purple":{type:"preset",key:"grad-purple"}, "grad-blue":{type:"preset",key:"grad-blue"}, "grad-pink":{type:"preset",key:"grad-pink"}, dots:{type:"preset",key:"dots"}, grid:{type:"preset",key:"grid"} };

function applyWallpaperStyle(presetOrObj){
    if(!chatMessages) return;
    // reset về mặc định của theme (không ép màu nền bằng inline)
    chatMessages.style.backgroundImage=""; chatMessages.style.backgroundSize=""; chatMessages.style.backgroundPosition=""; chatMessages.style.backgroundAttachment=""; chatMessages.style.backgroundColor="";

    if(!presetOrObj) return;
    if(presetOrObj.type==="url"){
        chatMessages.style.backgroundImage=`url("${presetOrObj.url}")`;
        chatMessages.style.backgroundSize="cover"; chatMessages.style.backgroundPosition="center"; chatMessages.style.backgroundAttachment="fixed";
        return;
    }
    switch(presetOrObj.key){
        case "grad-purple": chatMessages.style.backgroundImage="linear-gradient(135deg,#f5e1ff,#e7d4ff)"; break;
        case "grad-blue":   chatMessages.style.backgroundImage="linear-gradient(135deg,#dbeafe,#bfdbfe)"; break;
        case "grad-pink":   chatMessages.style.backgroundImage="linear-gradient(135deg,#ffe4e6,#fecdd3)"; break;
        case "dots":
            chatMessages.style.backgroundImage='radial-gradient(#e5e7eb 1.2px, transparent 1.2px), radial-gradient(#e5e7eb 1.2px, transparent 1.2px)';
            chatMessages.style.backgroundSize='20px 20px,20px 20px'; chatMessages.style.backgroundPosition='0 0,10px 10px'; chatMessages.style.backgroundColor='';
            break;
        case "grid":
            chatMessages.style.backgroundImage='linear-gradient(rgba(0,0,0,.06) 1px, transparent 1px), linear-gradient(90deg, rgba(0,0,0,.06) 1px, transparent 1px)';
            chatMessages.style.backgroundSize='24px 24px,24px 24px'; chatMessages.style.backgroundColor='';
            break;
        case "none":
        default: /* giữ nguyên theo theme */ break;
    }
}
function setActivePresetButton(key){ document.querySelectorAll('#wpPresets .wp-item').forEach(btn=>btn.setAttribute('aria-pressed', btn.getAttribute('data-wp')===key?'true':'false')); }
function saveWallpaper(data){ localStorage.setItem('chatWallpaper', JSON.stringify(data)); }
function loadWallpaper(){ try{ return JSON.parse(localStorage.getItem('chatWallpaper')||'null'); }catch{ return null; } }

(function initWallpaper(){
    const saved=loadWallpaper();
    if(saved){ applyWallpaperStyle(saved); if(saved.type==="preset") setActivePresetButton(saved.key); else setActivePresetButton(''); }
    else setActivePresetButton('none');
})();
wpPresetsEl?.addEventListener('click', (e)=>{
    const target=e.target.closest('.wp-item'); if(!target) return;
    const key=target.getAttribute('data-wp'); const data=WP_PRESETS[key]||WP_PRESETS.none;
    applyWallpaperStyle(data); setActivePresetButton(key); saveWallpaper(data);
});
applyWpUrlBtn?.addEventListener('click', ()=>{
    const url=(wpUrlInput?.value||'').trim(); if(!url) return;
    const data={type:'url', url}; applyWallpaperStyle(data); setActivePresetButton(''); saveWallpaper(data);
});
resetWpBtn?.addEventListener('click', ()=>{
    const data=WP_PRESETS.none; applyWallpaperStyle(data); setActivePresetButton('none'); saveWallpaper(data); if(wpUrlInput) wpUrlInput.value='';
});

/* ================= Emoji Picker ================= */
const bigEmojiToggle=$("#bigEmojiToggle");
(function initBigEmoji(){ const v=localStorage.getItem('bigEmoji')==='1'; if(bigEmojiToggle) bigEmojiToggle.checked=v; document.body.classList.toggle('big-emoji', v); })();
bigEmojiToggle?.addEventListener('change', (e)=>{ const v=e.target.checked; localStorage.setItem('bigEmoji', v?'1':'0'); document.body.classList.toggle('big-emoji', v); });

const chatTitle=$("#chatTitle"), chatNicknameInput=$("#chatNicknameInput"), applyNickname=$("#applyNickname");
(function initNickname(){ const nick=localStorage.getItem('chatNickname'); if(nick&&chatTitle&&chatNicknameInput){ chatTitle.textContent=nick; chatNicknameInput.value=nick; }})();
applyNickname?.addEventListener('click', ()=>{ const nick=(chatNicknameInput?.value||'').trim(); if(nick){ if(chatTitle) chatTitle.textContent=nick; localStorage.setItem('chatNickname', nick); } else { if(chatTitle) chatTitle.textContent='WebChat Pro'; localStorage.removeItem('chatNickname'); } });

const blockToggle=$("#blockToggle");
function applyBlockState(v){ if(!messageInput||!sendButton) return; messageInput.disabled=v; sendButton.disabled=v; messageInput.classList.toggle('opacity-60', v); sendButton.classList.toggle('opacity-60', v); messageInput.classList.toggle('cursor-not-allowed', v); sendButton.classList.toggle('cursor-not-allowed', v); }
(function initBlock(){ const blocked=localStorage.getItem('chatBlocked')==='1'; if(blockToggle) blockToggle.checked=blocked; applyBlockState(blocked); })();
blockToggle?.addEventListener('change', (e)=>{ const v=e.target.checked; localStorage.setItem('chatBlocked', v?'1':'0'); applyBlockState(v); });

const muteToggle=$("#muteToggle");
(function initMute(){ const muted=localStorage.getItem('chatMuted')==='1'; if(muteToggle) muteToggle.checked=muted; })();
muteToggle?.addEventListener('change', (e)=>{ const v=e.target.checked; localStorage.setItem('chatMuted', v?'1':'0'); });

const clearChatBtn=$("#clearChatBtn");
clearChatBtn?.addEventListener('click', ()=>{
    if(!chatMessages||!typingIndicator) return;
    Array.from(chatMessages.children).forEach(n=>{ if(n!==typingIndicator) n.remove(); });
    const info=document.createElement('div'); info.className='flex justify-center';
    info.innerHTML='<div class="glass-effect px-6 py-3 rounded-full text-sm text-gray-600 dark:text-gray-300">Đoạn chat đã được xóa 🗑️</div>';
    chatMessages.appendChild(info); closeChatSettingsPanel();
});

/* =============== Emoji picker core =============== */
const emojiToggleBtn=$("#emojiToggleBtn"), emojiPicker=$("#emojiPicker"), emojiGrid=$("#emojiGrid");
const EMOJI_CATEGORIES={
    camxuc:"😀 😃 😄 😁 😆 😅 😂 🙂 🙃 😊 😇 😉 😍 🥰 😘 😗 😙 😚 🤗 🤩 🤔 🤨 😐 😑 😶 🙄 😏 😣 😥 😮 🤐 😯 😪 😫 🥱 😴 😌 😛 😝 😜 🤪 🤭 🤫 🤥 😬 🫠 😳 🥵 🥶 🥴 😵 🤯 🤠 🥳 😎 🤓 🧐 😕 😟 🙁 ☹️ 😮‍💨 😤 😢 😭 😖 😞 😓 😩 🤬 🤧 🤮 🤢 🤒 🤕 🥺 🙏".split(" "),
    cucchi:"👍 👎 👋 🤚 ✋ 🖐 🖖 👌 🤌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🙏 ✍️ 💅 🤳".split(" "),
    dongvat:"🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🐔 🐧 🐦 🐤 🐣 🐥 🐺 🦄 🐝 🐛 🦋 🐌 🐞 🪲 🐢 🐍 🐙 🐠 🐟 🐬 🐳 🐋 🐊 🦖".split(" "),
    doan:"🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🫐 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🥑 🍆 🥔 🥕 🌽 🌶️ 🧄 🧅 🥬 🥦 🍄 🥜 🍞 🥐 🥖 🥯 🥞 🧇 🧀 🍗 🍖 🍤 🍣 🍕 🍔 🍟 🌭 🥪 🌮 🌯 🥗 🍝 🍜 🍲 🍥 🥮 🍡 🍦 🍰 🎂 🍩 🍪 🍫 🍬 🍭 🍯 🍼 ☕ 🍵 🧋 🥤 🍻 🍷 🥂 🍹".split(" "),
    hoatdong:"⚽ 🏀 🏈 ⚾ 🎾 🏐 🏉 🎱 🏓 🏸 🥅 🥊 🥋 ⛳ 🏒 🏑 🥍 🛹 🎿 ⛷️ 🏂 🏋️‍♀️ 🤼‍♂️ 🤺 🤾‍♂️ 🧗‍♀️ 🧘‍♂️ 🏄‍♀️ 🚴‍♂️ 🚵‍♀️ 🏇 🎯 🎮 🎲 🎻 🎸 🎺 🎷 🥁 🎤 🎧".split(" ")
};

function getRecentEmojis(){ try{ return JSON.parse(localStorage.getItem('recentEmojis')||'[]'); }catch{ return []; } }
function saveRecentEmoji(e){ let arr=getRecentEmojis().filter(x=>x!==e); arr.unshift(e); if(arr.length>24) arr=arr.slice(0,24); localStorage.setItem('recentEmojis', JSON.stringify(arr)); }

function renderEmojiGrid(cat='recent'){
    if(!emojiGrid) return; let list=[];
    if(cat==='recent') list=getRecentEmojis();
    if(!list||list.length===0){ cat=cat==='recent'?'camxuc':cat; list=EMOJI_CATEGORIES[cat]||[]; }
    emojiGrid.innerHTML=''; list.forEach(e=>{
        const btn=document.createElement('button'); btn.type='button'; btn.className='emoji-btn'; btn.textContent=e;
        btn.addEventListener('click',()=>{ insertAtCursor(messageInput, e); saveRecentEmoji(e); messageInput?.focus(); });
        emojiGrid.appendChild(btn);
    });
}
function setActiveTab(cat){ document.querySelectorAll('#emojiPicker .emoji-tab').forEach(t=>t.setAttribute('aria-selected', t.getAttribute('data-cat')===cat?'true':'false')); renderEmojiGrid(cat); }
function insertAtCursor(input, text){ if(!input) return; const s=input.selectionStart??input.value.length, e=input.selectionEnd??input.value.length; const before=input.value.slice(0,s), after=input.value.slice(e); input.value=before+text+after; const pos=s+text.length; input.setSelectionRange(pos,pos); }
function openEmojiPicker(){ if(!emojiPicker||!emojiToggleBtn) return; emojiPicker.classList.remove('hidden'); emojiToggleBtn.setAttribute('aria-expanded','true'); setActiveTab('recent'); }
function closeEmojiPicker(){ if(!emojiPicker||!emojiToggleBtn) return; emojiPicker.classList.add('hidden'); emojiToggleBtn.setAttribute('aria-expanded','false'); }
emojiToggleBtn?.addEventListener('click',(e)=>{ e.stopPropagation(); if(emojiPicker?.classList.contains('hidden')) openEmojiPicker(); else closeEmojiPicker(); });
emojiPicker?.addEventListener('click',(e)=>{ const tab=e.target.closest('.emoji-tab'); if(tab) setActiveTab(tab.getAttribute('data-cat')); });
document.addEventListener('click',(e)=>{ if(!emojiPicker||!emojiToggleBtn) return; if(!emojiPicker.classList.contains('hidden')){ if(!emojiPicker.contains(e.target) && e.target!==emojiToggleBtn){ closeEmojiPicker(); } } });
window.addEventListener('keydown',(e)=>{ if(e.key==='Escape') closeEmojiPicker(); });
renderEmojiGrid('recent');



//tìm kiếm theo chữ cái
document.addEventListener("DOMContentLoaded", () => {
    const searchInput = document.getElementById("chatSearchInput");
    const chatList = document.getElementById("chatList");

    if (!searchInput || !chatList) return;

    const normalize = (s) =>
        (s || "")
            .toString()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase();

    searchInput.addEventListener("input", () => {
        const keyword = normalize(searchInput.value);
        const items = Array.from(chatList.querySelectorAll(".chat-item"));

        // Tách 2 nhóm: bắt đầu bằng keyword, và chỉ chứa
        const startsWith = [];
        const contains = [];

        items.forEach((item) => {
            const nameEl = item.querySelector("h3, .chat-name");
            const name = nameEl ? normalize(nameEl.textContent) : "";

            if (!keyword) {
                item.classList.remove("hidden");
                contains.push(item); // mặc định trả lại đúng thứ tự ban đầu
                return;
            }

            if (name.startsWith(keyword)) {
                item.classList.remove("hidden");
                startsWith.push(item);
            } else if (name.includes(keyword)) {
                item.classList.remove("hidden");
                contains.push(item);
            } else {
                item.classList.add("hidden");
            }
        });

        // Gom nhóm: bắt đầu trước, chứa sau
        [...startsWith, ...contains].forEach((el) => chatList.appendChild(el));
    });
});


//-------------------------- gửi ảnh ----------------------//

const lb = document.getElementById("imgLightbox");
const lbImg = document.getElementById("imgLightboxImg");
function openLightbox(src) {
    if (!lb || !lbImg) return;
    lbImg.src = src;
    lb.classList.remove("hidden");
}
lb?.addEventListener("click", () => lb.classList.add("hidden")); // bấm nền để đóng


function addImageMessage(src) {
    if (!chatMessages || !typingIndicator) return;

    const wrap = document.createElement("div");
    wrap.className = "flex items-end justify-end space-x-2 message-bubble";

    const bubble = document.createElement("div");
    bubble.className = "flex flex-col items-end gap-1";

    const img = document.createElement("img");
    img.src = src;
    img.alt = "image";
    img.className = "max-w-[200px] rounded-xl shadow cursor-zoom-in";
    img.addEventListener("click", () => openLightbox(src));

    const time = document.createElement("span");
    time.className = "text-xs text-gray-400";
    time.textContent = new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });

    bubble.appendChild(img);
    bubble.appendChild(time);

    const avatar = document.createElement("div");
    avatar.className = "w-8 h-8 bg-gradient-to-r from-purple-400 to-pink-500 rounded-full flex items-center justify-center";
    avatar.innerHTML = `<span class="text-white text-sm">👤</span>`;

    wrap.appendChild(bubble);
    wrap.appendChild(avatar);

    chatMessages.insertBefore(wrap, typingIndicator);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

attachBtn?.addEventListener("click", () => fileInput?.click());

fileInput?.addEventListener("change", (e) => {
    const files = Array.from(e.target.files || []).filter(f => f.type.startsWith("image/"));
    files.forEach(file => {
        const reader = new FileReader();
        reader.onload = () => addImageMessage(reader.result);
        reader.readAsDataURL(file);
    });
    fileInput.value = "";
});
