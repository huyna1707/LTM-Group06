
/* ------------ Helper truy cập DOM an toàn ------------ */
const $ = (sel) => document.querySelector(sel);

/* ------------ Theme Toggle (Sáng/Tối toàn app) ------------ */
const themeToggle = $("#themeToggle");
const lightIcon   = $("#lightIcon");
const darkIcon    = $("#darkIcon");
const html        = document.documentElement;

const currentTheme = localStorage.getItem("theme") || "light";
if (currentTheme === "dark") html.classList.add("dark");
else html.classList.remove("dark");

function updateThemeIcons({ flashSun = false } = {}) {
    const chatContainer = $(".chat-container");
    if (!chatContainer) return;

    if (html.classList.contains("dark")) {
        // ĐANG TỐI -> hiện ☀️ để bấm chuyển sang SÁNG
        lightIcon?.classList.add("hidden");
        darkIcon?.classList.remove("hidden");

        themeToggle?.classList.remove("sun-flash","sun-flash-anim","bg-white","ring-2","ring-white","shadow-lg");
        themeToggle?.classList.add("dark:bg-gray-800");

        chatContainer.style.background = "linear-gradient(135deg, #2d1b4e 0%, #1a102b 100%)";
    } else {
        // ĐANG SÁNG -> hiện 🌙 để bấm chuyển sang TỐI
        lightIcon?.classList.remove("hidden");
        darkIcon?.classList.add("hidden");

        // Nút "mặt trời" phát sáng khi vừa chuyển sang sáng
        themeToggle?.classList.add("bg-white","ring-2","ring-white","shadow-lg");
        if (flashSun) {
            themeToggle?.classList.add("sun-flash","sun-flash-anim");
            setTimeout(() => themeToggle?.classList.remove("sun-flash","sun-flash-anim"), 480);
        }

        chatContainer.style.background = "linear-gradient(135deg, #a78bfa 0%, #7c3aed 100%)";
    }
}
updateThemeIcons();

themeToggle?.addEventListener("click", () => {
    const wasDark = html.classList.contains("dark");
    html.classList.toggle("dark");
    const newTheme = html.classList.contains("dark") ? "dark" : "light";
    localStorage.setItem("theme", newTheme);
    updateThemeIcons({ flashSun: wasDark });
});

/* ------------ Message Functionality ------------ */
const messageInput   = $("#messageInput");
const sendButton     = $("#sendButton");
const chatMessages   = $("#chatMessages");
const typingIndicator= $("#typingIndicator");

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

function showTyping() { typingIndicator?.classList.remove("hidden"); chatMessages && (chatMessages.scrollTop = chatMessages.scrollHeight); }
function hideTyping() { typingIndicator?.classList.add("hidden"); }

function sendMessage() {
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
    }, 1500);
}

sendButton?.addEventListener("click", sendMessage);
messageInput?.addEventListener("keypress", (e) => { if (e.key === "Enter") sendMessage(); });
messageInput?.focus();

/* ------------ Modal helpers ------------ */
function openModal(id)  { document.getElementById(id)?.classList.remove("hidden"); }
function closeModal(id) { document.getElementById(id)?.classList.add("hidden"); }

$("#addFriendBtn")   ?.addEventListener("click", () => openModal("addFriendModal"));
$("#createGroupBtn") ?.addEventListener("click", () => openModal("createGroupModal"));

/* ------------ Helpers chung ------------ */
function timeNowLabel() {
    const d = new Date();
    return d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
}

function getInitials(name) {
    if (!name) return "UU";
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

const gradients = [
    "from-green-500 to-teal-500",
    "from-blue-500 to-indigo-500",
    "from-red-500 to-pink-500",
    "from-purple-500 to-pink-500",
    "from-emerald-500 to-cyan-500",
    "from-amber-500 to-orange-600"
];
function pickGradient(seed = 0) { return gradients[seed % gradients.length]; }

function createChatItem({ name, preview = "Tin nhắn mới", unread = true, power = 1, gradientSeed = 0 }) {
    const initials = getInitials(name);
    const gradient = pickGradient(gradientSeed);
    const wrapper = document.createElement("div");
    wrapper.className = "p-3 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors";
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

// Confirm Add Friend
$("#confirmAddFriend")?.addEventListener("click", () => {
    const input = $("#friendInput");
    const value = (input?.value || "").trim();
    if (!value) return;
    const name = `Người dùng ${value}`;
    const node = createChatItem({
        name,
        preview: "Vừa được thêm vào danh bạ",
        unread: true,
        power: 1,
        gradientSeed: value.length
    });
    chatList?.prepend(node);
    closeModal("addFriendModal");
    if (input) input.value = "";
});

// Confirm Create Group
$("#confirmCreateGroup")?.addEventListener("click", () => {
    const nameEl = $("#groupName");
    const membersEl = $("#groupMembers");
    const name = (nameEl?.value || "").trim();
    const members = (membersEl?.value || "").trim();
    if (!name || !members) return;

    const membersArr = members.split(",").map(s => s.trim()).filter(Boolean);
    const preview = `Thành viên: ${membersArr.slice(0, 3).join(", ")}${membersArr.length > 3 ? "..." : ""}`;

    const node = createChatItem({
        name,
        preview,
        unread: true,
        power: Math.max(1, Math.min(12, membersArr.length)),
        gradientSeed: name.length + membersArr.length
    });
    chatList?.prepend(node);

    closeModal("createGroupModal");
    if (nameEl) nameEl.value = "";
    if (membersEl) membersEl.value = "";
});

/* ------------ Edit Profile (status select + dot) ------------ */
const profileNameEl    = $("#profileName");
const profileStatusEl  = $("#profileStatus");
const editProfileBtn   = $("#editProfileBtn");
const editNameInput    = $("#editName");
const editStatusSelect = $("#editStatus");
const editPersistChk   = $("#editPersist");

const appStatusText = $("#appStatusText");
const appStatusDot  = $("#appStatusDot");
const profileDot    = $("#profileDot");

const STATUS_MAP = {
    active:  { label: "Đang hoạt động",             colorClass: "bg-green-500"  },
    busy:    { label: "Đang bận",                   colorClass: "bg-yellow-500" },
    offline: { label: "Tắt trạng thái hoạt động",   colorClass: "bg-red-500"    }
};
const STATUS_KEYS = Object.keys(STATUS_MAP);
const ALL_DOT_CLASSES = ["bg-green-500", "bg-yellow-500", "bg-red-500"];

function labelToKey(label = "") {
    label = (label || "").toLowerCase().trim();
    if (label.includes("bận")) return "busy";
    if (label.includes("tắt")) return "offline";
    return "active";
}

function updateDotsColor(dotEl, key) {
    if (!dotEl) return;
    dotEl.classList.remove(...ALL_DOT_CLASSES);
    dotEl.classList.add(STATUS_MAP[key].colorClass);
}

function updateStatusUI(key) {
    const { label } = STATUS_MAP[key] || STATUS_MAP.active;
    if (profileStatusEl) profileStatusEl.textContent = label;
    if (appStatusText)   appStatusText.textContent  = label;
    updateDotsColor(profileDot, key);
    updateDotsColor(appStatusDot, key);
}

// Init từ LocalStorage
(function initProfileFromStorage(){
    const nameFromLS   = localStorage.getItem("profileName");
    const statusKeyLS  = localStorage.getItem("profileStatusKey");
    const statusTextLS = localStorage.getItem("profileStatus"); // backward compatible

    if (nameFromLS && profileNameEl) profileNameEl.textContent = nameFromLS;

    let initKey = "active";
    if (statusKeyLS && STATUS_MAP[statusKeyLS]) initKey = statusKeyLS;
    else if (statusTextLS) initKey = labelToKey(statusTextLS);

    updateStatusUI(initKey);
    if (editStatusSelect) editStatusSelect.value = initKey;
})();

/* ------------ AVATAR: preview + lưu localStorage ------------ */
const profileAvatarImg      = $("#profileAvatarImg");
const profileAvatarFallback = $("#profileAvatarFallback");

const editAvatarPreview = $("#editAvatarPreview");
const editAvatarFallback= $("#editAvatarFallback");
const editAvatarBtn     = $("#editAvatarBtn");
const editAvatarFile    = $("#editAvatarFile");
const removeAvatarBtn   = $("#removeAvatarBtn");

function applyAvatar(dataUrl) {
    // Sidebar
    if (profileAvatarImg && profileAvatarFallback) {
        if (dataUrl) {
            profileAvatarImg.src = dataUrl;
            profileAvatarImg.style.display = "block";
            profileAvatarImg.classList.add("w-12","h-12");
            profileAvatarFallback.style.display = "none";
        } else {
            profileAvatarImg.removeAttribute("src");
            profileAvatarImg.style.display = "none";
            profileAvatarFallback.style.display = "flex";
        }
    }
    // Modal preview
    if (editAvatarPreview && editAvatarFallback) {
        if (dataUrl) {
            editAvatarPreview.src = dataUrl;
            editAvatarPreview.style.display = "block";
            editAvatarFallback.style.display = "none";
        } else {
            editAvatarPreview.removeAttribute("src");
            editAvatarPreview.style.display = "none";
            editAvatarFallback.style.display = "flex";
        }
    }
}

function fileToDataURL(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}

// Nạp avatar khi load trang
(function initAvatarFromStorage(){
    const saved = localStorage.getItem("profileAvatar") || "";
    applyAvatar(saved || null);
})();

/* ------------ MỞ MODAL SỬA HỒ SƠ (gộp 1 listener duy nhất) ------------ */
editProfileBtn?.addEventListener("click", () => {
    const currentName = localStorage.getItem("profileName") || (profileNameEl?.textContent?.trim() || "Bạn");
    const currentKey  = localStorage.getItem("profileStatusKey") || labelToKey(profileStatusEl?.textContent?.trim() || "");

    if (editNameInput)    editNameInput.value = currentName;
    if (editStatusSelect) editStatusSelect.value = STATUS_KEYS.includes(currentKey) ? currentKey : "active";

    // load preview avatar hiện tại
    applyAvatar(localStorage.getItem("profileAvatar") || null);

    if (editPersistChk) {
        editPersistChk.checked = Boolean(
            localStorage.getItem("profileName") ||
            localStorage.getItem("profileStatusKey") ||
            localStorage.getItem("profileStatus")
        );
    }
    openModal("editProfileModal");
});

// Lưu tên + trạng thái
$("#saveProfileChanges")?.addEventListener("click", () => {
    const newName = (editNameInput?.value.trim() || "Bạn");
    const key     = editStatusSelect?.value || "active";
    const safeKey = STATUS_KEYS.includes(key) ? key : "active";

    if (profileNameEl) profileNameEl.textContent = newName;
    updateStatusUI(safeKey);

    if (editPersistChk?.checked) {
        localStorage.setItem("profileName", newName);
        localStorage.setItem("profileStatusKey", safeKey);
        localStorage.setItem("profileStatus", STATUS_MAP[safeKey].label);
    } else {
        localStorage.removeItem("profileName");
        localStorage.removeItem("profileStatusKey");
        localStorage.removeItem("profileStatus");
    }
    closeModal("editProfileModal");
});

// Avatar: nút sửa -> chọn file
editAvatarBtn?.addEventListener("click", () => editAvatarFile?.click());

// Chọn file -> validate + preview + lưu
editAvatarFile?.addEventListener("change", async (e) => {
    const f = e.target.files?.[0];
    if (!f) return;
    if (f.size > 2 * 1200 * 1080) {
        alert("Ảnh quá lớn. Vui lòng chọn ảnh nhỏ hơn.");
        editAvatarFile.value = "";
        return;
    }
    try {
        const dataUrl = await fileToDataURL(f);
        localStorage.setItem("profileAvatar", dataUrl);
        applyAvatar(dataUrl);
    } catch (err) {
        console.error(err);
        alert("Không đọc được file ảnh.");
    }
});

// Xóa ảnh -> về mặc định
removeAvatarBtn?.addEventListener("click", () => {
    localStorage.removeItem("profileAvatar");
    if (editAvatarFile) editAvatarFile.value = "";
    applyAvatar(null);
});

/* ------------ Chat Settings Drawer ------------ */
const chatSettingsBtn   = $("#chatSettingsBtn");
const chatSettingsPanel = $("#chatSettingsPanel");
const chatSettingsOverlay = $("#chatSettingsOverlay");
const closeChatSettings = $("#closeChatSettings");
const mainChatArea      = $("#mainChatArea");

function openChatSettings() {
    chatSettingsPanel?.classList.remove('translate-x-full');
    chatSettingsOverlay?.classList.remove('hidden');
    mainChatArea?.classList.add('mr-80');
}
function closeChatSettingsPanel() {
    chatSettingsPanel?.classList.add('translate-x-full');
    chatSettingsOverlay?.classList.add('hidden');
    mainChatArea?.classList.remove('mr-80');
}
chatSettingsBtn   ?.addEventListener('click', openChatSettings);
closeChatSettings ?.addEventListener('click', closeChatSettingsPanel);
chatSettingsOverlay?.addEventListener('click', closeChatSettingsPanel);
window.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeChatSettingsPanel(); });

/* ------------ Wallpaper (Hình nền đoạn chat) ------------ */
const wpPresetsEl  = $("#wpPresets");
const wpUrlInput   = $("#wpUrl");
const applyWpUrlBtn= $("#applyWpUrl");
const resetWpBtn   = $("#resetWp");

const WP_PRESETS = {
    none:          { type: 'preset', key: 'none' },
    'grad-purple': { type: 'preset', key: 'grad-purple' },
    'grad-blue':   { type: 'preset', key: 'grad-blue' },
    'grad-pink':   { type: 'preset', key: 'grad-pink' },
    dots:          { type: 'preset', key: 'dots' },
    grid:          { type: 'preset', key: 'grid' },
};

function applyWallpaperStyle(presetOrObj) {
    if (!chatMessages) return;

    // reset
    chatMessages.style.backgroundImage = 'none';
    chatMessages.style.backgroundSize = '';
    chatMessages.style.backgroundPosition = '';
    chatMessages.style.backgroundAttachment = '';
    chatMessages.style.backgroundColor = '';

    if (!presetOrObj) return;

    if (presetOrObj.type === 'url') {
        chatMessages.style.backgroundImage = `url("${presetOrObj.url}")`;
        chatMessages.style.backgroundSize = 'cover';
        chatMessages.style.backgroundPosition = 'center';
        chatMessages.style.backgroundAttachment = 'fixed';
        return;
    }

    switch (presetOrObj.key) {
        case 'grad-purple':
            chatMessages.style.backgroundImage = 'linear-gradient(135deg,#f5e1ff,#e7d4ff)';
            break;
        case 'grad-blue':
            chatMessages.style.backgroundImage = 'linear-gradient(135deg,#dbeafe,#bfdbfe)';
            break;
        case 'grad-pink':
            chatMessages.style.backgroundImage = 'linear-gradient(135deg,#ffe4e6,#fecdd3)';
            break;
        case 'dots':
            chatMessages.style.backgroundImage =
                'radial-gradient(#e5e7eb 1.2px, transparent 1.2px), radial-gradient(#e5e7eb 1.2px, transparent 1.2px)';
            chatMessages.style.backgroundSize = '20px 20px,20px 20px';
            chatMessages.style.backgroundPosition = '0 0,10px 10px';
            chatMessages.style.backgroundColor = '#ffffff';
            break;
        case 'grid':
            chatMessages.style.backgroundImage =
                'linear-gradient(rgba(0,0,0,.06) 1px, transparent 1px), linear-gradient(90deg, rgba(0,0,0,.06) 1px, transparent 1px)';
            chatMessages.style.backgroundSize = '24px 24px,24px 24px';
            chatMessages.style.backgroundColor = '#ffffff';
            break;
        case 'none':
        default:
            break;
    }
}

function setActivePresetButton(key) {
    document.querySelectorAll('#wpPresets .wp-item').forEach(btn => {
        btn.setAttribute('aria-pressed', btn.getAttribute('data-wp') === key ? 'true' : 'false');
    });
}
function saveWallpaper(data) { localStorage.setItem('chatWallpaper', JSON.stringify(data)); }
function loadWallpaper() { try { return JSON.parse(localStorage.getItem('chatWallpaper') || 'null'); } catch { return null; } }

// Init wallpaper
(function initWallpaper(){
    const saved = loadWallpaper();
    if (saved) {
        applyWallpaperStyle(saved);
        if (saved.type === 'preset') setActivePresetButton(saved.key);
        else setActivePresetButton('');
    } else {
        setActivePresetButton('none');
    }
})();

wpPresetsEl?.addEventListener('click', (e) => {
    const target = e.target.closest('.wp-item');
    if (!target) return;
    const key = target.getAttribute('data-wp');
    const data = WP_PRESETS[key] || WP_PRESETS.none;
    applyWallpaperStyle(data);
    setActivePresetButton(key);
    saveWallpaper(data);
});

applyWpUrlBtn?.addEventListener('click', () => {
    const url = (wpUrlInput?.value || '').trim();
    if (!url) return;
    const data = { type: 'url', url };
    applyWallpaperStyle(data);
    setActivePresetButton('');
    saveWallpaper(data);
});

resetWpBtn?.addEventListener('click', () => {
    const data = WP_PRESETS.none;
    applyWallpaperStyle(data);
    setActivePresetButton('none');
    saveWallpaper(data);
    if (wpUrlInput) wpUrlInput.value = '';
});

/* ------------ Biểu tượng cảm xúc lớn ------------ */
const bigEmojiToggle = $("#bigEmojiToggle");
(function initBigEmoji() {
    const v = localStorage.getItem('bigEmoji') === '1';
    if (bigEmojiToggle) bigEmojiToggle.checked = v;
    document.body.classList.toggle('big-emoji', v);
})();
bigEmojiToggle?.addEventListener('change', (e) => {
    const v = e.target.checked;
    localStorage.setItem('bigEmoji', v ? '1' : '0');
    document.body.classList.toggle('big-emoji', v);
});

/* ------------ Biệt danh (nickname) ------------ */
const chatTitle         = $("#chatTitle");
const chatNicknameInput = $("#chatNicknameInput");
const applyNickname     = $("#applyNickname");

(function initNickname() {
    const nick = localStorage.getItem('chatNickname');
    if (nick && chatTitle && chatNicknameInput) {
        chatTitle.textContent = nick;
        chatNicknameInput.value = nick;
    }
})();
applyNickname?.addEventListener('click', () => {
    const nick = (chatNicknameInput?.value || '').trim();
    if (nick) {
        if (chatTitle) chatTitle.textContent = nick;
        localStorage.setItem('chatNickname', nick);
    } else {
        if (chatTitle) chatTitle.textContent = 'WebChat Pro';
        localStorage.removeItem('chatNickname');
    }
});

/* ------------ Chặn / Tắt thông báo ------------ */
const blockToggle = $("#blockToggle");
function applyBlockState(v) {
    if (!messageInput || !sendButton) return;
    messageInput.disabled = v;
    sendButton.disabled   = v;
    messageInput.classList.toggle('opacity-60', v);
    sendButton .classList.toggle('opacity-60', v);
    messageInput.classList.toggle('cursor-not-allowed', v);
    sendButton .classList.toggle('cursor-not-allowed', v);
}
(function initBlock() {
    const blocked = localStorage.getItem('chatBlocked') === '1';
    if (blockToggle) blockToggle.checked = blocked;
    applyBlockState(blocked);
})();
blockToggle?.addEventListener('change', (e) => {
    const v = e.target.checked;
    localStorage.setItem('chatBlocked', v ? '1' : '0');
    applyBlockState(v);
});

const muteToggle = $("#muteToggle");
(function initMute() {
    const muted = localStorage.getItem('chatMuted') === '1';
    if (muteToggle) muteToggle.checked = muted;
})();
muteToggle?.addEventListener('change', (e) => {
    const v = e.target.checked;
    localStorage.setItem('chatMuted', v ? '1' : '0');
});

/* ------------ Xóa đoạn chat ------------ */
const clearChatBtn = $("#clearChatBtn");
clearChatBtn?.addEventListener('click', () => {
    if (!chatMessages || !typingIndicator) return;
    Array.from(chatMessages.children).forEach(node => {
        if (node !== typingIndicator) node.remove();
    });
    const info = document.createElement('div');
    info.className = 'flex justify-center';
    info.innerHTML = '<div class="glass-effect px-6 py-3 rounded-full text-sm text-gray-600 dark:text-gray-300">Đoạn chat đã được xóa 🗑️</div>';
    chatMessages.appendChild(info);
    closeChatSettingsPanel();
});

/* =========================================================
   EMOJI PICKER
========================================================= */
const emojiToggleBtn = $("#emojiToggleBtn");
const emojiPicker    = $("#emojiPicker");
const emojiGrid      = $("#emojiGrid");

const EMOJI_CATEGORIES = {
    camxuc: "😀 😃 😄 😁 😆 😅 😂 🙂 🙃 😊 😇 😉 😍 🥰 😘 😗 😙 😚 🤗 🤩 🤔 🤨 😐 😑 😶 🙄 😏 😣 😥 😮 🤐 😯 😪 😫 🥱 😴 😌 😛 😝 😜 🤪 🤭 🤫 🤥 😬 🫠 😳 🥵 🥶 🥴 😵 🤯 🤠 🥳 😎 🤓 🧐 😕 😟 🙁 ☹️ 😮‍💨 😤 😢 😭 😖 😞 😓 😩 🤬 🤧 🤮 🤢 🤒 🤕 🥺 🙏".split(" "),
    cucchi: "👍 👎 👋 🤚 ✋ 🖐 🖖 👌 🤌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🙏 ✍️ 💅 🤳".split(" "),
    dongvat:"🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🐔 🐧 🐦 🐤 🐣 🐥 🐺 🦄 🐝 🐛 🦋 🐌 🐞 🪲 🐢 🐍 🐙 🐠 🐟 🐬 🐳 🐋 🐊 🦖".split(" "),
    doan:   "🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🫐 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🥑 🍆 🥔 🥕 🌽 🌶️ 🧄 🧅 🥬 🥦 🍄 🥜 🍞 🥐 🥖 🥯 🥞 🧇 🧀 🍗 🍖 🍤 🍣 🍕 🍔 🍟 🌭 🥪 🌮 🌯 🥗 🍝 🍜 🍲 🍥 🥮 🍡 🍦 🍰 🎂 🍩 🍪 🍫 🍬 🍭 🍯 🍼 ☕ 🍵 🧋 🥤 🍻 🍷 🥂 🍹".split(" "),
    hoatdong:"⚽ 🏀 🏈 ⚾ 🎾 🏐 🏉 🎱 🏓 🏸 🥅 🥊 🥋 ⛳ 🏒 🏑 🥍 🛹 🎿 ⛷️ 🏂 🏋️‍♀️ 🤼‍♂️ 🤺 🤾‍♂️ 🧗‍♀️ 🧘‍♂️ 🏄‍♀️ 🚴‍♂️ 🚵‍♀️ 🏇 🎯 🎮 🎲 🎻 🎸 🎺 🎷 🥁 🎤 🎧".split(" ")
};

// Lưu thường dùng
function getRecentEmojis() {
    try { return JSON.parse(localStorage.getItem('recentEmojis') || '[]'); } catch { return []; }
}
function saveRecentEmoji(e) {
    let arr = getRecentEmojis().filter(x => x !== e);
    arr.unshift(e);
    if (arr.length > 24) arr = arr.slice(0, 24);
    localStorage.setItem('recentEmojis', JSON.stringify(arr));
}

// render grid theo cat
function renderEmojiGrid(cat = 'recent') {
    if (!emojiGrid) return;
    let list = [];
    if (cat === 'recent') list = getRecentEmojis();
    if (!list || list.length === 0) {
        cat = cat === 'recent' ? 'camxuc' : cat;
        list = EMOJI_CATEGORIES[cat] || [];
    }
    emojiGrid.innerHTML = '';
    list.forEach(e => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'emoji-btn';
        btn.textContent = e;
        btn.addEventListener('click', () => {
            insertAtCursor(messageInput, e);
            saveRecentEmoji(e);
            messageInput?.focus();
        });
        emojiGrid.appendChild(btn);
    });
}

function setActiveTab(cat) {
    document.querySelectorAll('#emojiPicker .emoji-tab').forEach(t => {
        t.setAttribute('aria-selected', t.getAttribute('data-cat') === cat ? 'true' : 'false');
    });
    renderEmojiGrid(cat);
}

// chèn vào vị trí con trỏ
function insertAtCursor(input, text) {
    if (!input) return;
    const start = input.selectionStart ?? input.value.length;
    const end   = input.selectionEnd   ?? input.value.length;
    const before = input.value.slice(0, start);
    const after  = input.value.slice(end);
    input.value = before + text + after;
    const newPos = start + text.length;
    input.setSelectionRange(newPos, newPos);
}

// mở/đóng picker
function openEmojiPicker() {
    if (!emojiPicker || !emojiToggleBtn) return;
    emojiPicker.classList.remove('hidden');
    emojiToggleBtn.setAttribute('aria-expanded', 'true');
    setActiveTab('recent');
}
function closeEmojiPicker() {
    if (!emojiPicker || !emojiToggleBtn) return;
    emojiPicker.classList.add('hidden');
    emojiToggleBtn.setAttribute('aria-expanded', 'false');
}

emojiToggleBtn?.addEventListener('click', (e) => {
    e.stopPropagation();
    if (emojiPicker?.classList.contains('hidden')) openEmojiPicker();
    else closeEmojiPicker();
});

// click tab
emojiPicker?.addEventListener('click', (e) => {
    const tab = e.target.closest('.emoji-tab');
    if (tab) setActiveTab(tab.getAttribute('data-cat'));
});

// đóng khi click ngoài
document.addEventListener('click', (e) => {
    if (!emojiPicker || !emojiToggleBtn) return;
    if (!emojiPicker.classList.contains('hidden')) {
        if (!emojiPicker.contains(e.target) && e.target !== emojiToggleBtn) {
            closeEmojiPicker();
        }
    }
});
// ESC để đóng
window.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeEmojiPicker(); });

// init lần đầu
renderEmojiGrid('recent');
