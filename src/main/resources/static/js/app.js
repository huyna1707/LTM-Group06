'use strict';

/* ========================================================
   DOM HELPERS & GLOBALS
======================================================== */
const $ = (sel) => document.querySelector(sel);

// CSRF
const csrfToken = document.querySelector("meta[name='_csrf']")?.content;
const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.content;

// Main elements
const usernamePage = $('#username-page');
const chatPage = $('#chat-page');
const messageInput = $("#messageInput");
const sendButton = $("#sendButton");
const chatMessages = $("#chatMessages");

let stompClient = null;
let username = null;
let currentChat = null; // {type: 'public'|'private'|'group', id, name}

/* ========================================================
   VISUAL / THEME UTILITIES (merged from file)
======================================================== */
const themeToggle = $("#themeToggle");
const lightIcon   = $("#lightIcon");
const darkIcon    = $("#darkIcon");
const html        = document.documentElement;

// init theme from localStorage
const _initialTheme = localStorage.getItem("theme") || "light";
if (_initialTheme === "dark") html.classList.add("dark"); else html.classList.remove("dark");

function updateThemeIcons({ flashSun = false } = {}) {
  const chatContainer = $(".chat-container");
  if (html.classList.contains("dark")) {
    lightIcon?.classList.add("hidden");
    darkIcon?.classList.remove("hidden");
    themeToggle?.classList.remove("sun-flash","sun-flash-anim","bg-white","ring-2","ring-white","shadow-lg");
    themeToggle?.classList.add("dark:bg-gray-800");
    if (chatContainer) chatContainer.style.background = "linear-gradient(135deg, #2d1b4e 0%, #1a102b 100%)";
  } else {
    lightIcon?.classList.remove("hidden");
    darkIcon?.classList.add("hidden");
    themeToggle?.classList.add("bg-white","ring-2","ring-white","shadow-lg");
    if (flashSun) {
      themeToggle?.classList.add("sun-flash","sun-flash-anim");
      setTimeout(() => themeToggle?.classList.remove("sun-flash","sun-flash-anim"), 480);
    }
    if (chatContainer) chatContainer.style.background = "linear-gradient(135deg, #a78bfa 0%, #7c3aed 100%)";
  }
}
updateThemeIcons();

themeToggle?.addEventListener("click", () => {
  const wasDark = html.classList.contains("dark");
  html.classList.toggle("dark");
  localStorage.setItem("theme", html.classList.contains("dark") ? "dark" : "light");
  updateThemeIcons({ flashSun: wasDark });
});

/* ========================================================
   NAME/INITIALS + GRADIENT HELPERS (shared)
======================================================== */
const gradients = [
  "from-green-500 to-teal-500", "from-blue-500 to-indigo-500",
  "from-red-500 to-pink-500", "from-purple-500 to-pink-500",
  "from-emerald-500 to-cyan-500", "from-amber-500 to-orange-600"
];
function pickGradient(seed = 0) { return gradients[seed % gradients.length]; }
function getInitials(name) {
  if (!name) return "??";
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}
function simpleHash(str) {
  let hash = 0;
  for (let i = 0; i < str.length; i++) { hash = (hash << 5) - hash + str.charCodeAt(i); hash |= 0; }
  return Math.abs(hash);
}

/* ========================================================
   WEBSOCKET + STOMP
======================================================== */
function connect(event) {
  username = $('#username')?.value.trim();
  if (username) {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    const headers = {};
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
    stompClient.connect(headers, onConnected, onError);
  }
  event?.preventDefault();
}

function onConnected() {
  console.log('✅ Connected to WebSocket');

  // Subscriptions
  stompClient.subscribe('/topic/public', onPublicMessageReceived);
  stompClient.subscribe('/topic/user-status', onUserStatusChanged);
  stompClient.subscribe(`/user/${username}/private`, onPrivateMessageReceived);
  stompClient.subscribe(`/user/${username}/friend-request`, onFriendRequestReceived);
  stompClient.subscribe(`/user/${username}/group`, onGroupMessageReceived);

  // Load initial data
  loadInitialData();

  // JOIN
  stompClient.send('/app/chat.join', {}, JSON.stringify({ sender: username, type: 'JOIN' }));

  // default room
  switchToPublicChat();
}

function onError(error) {
  console.error('❌ WebSocket connection error:', error);
  showErrorMessage('Lỗi kết nối WebSocket. Vui lòng tải lại trang.');
}

/* ========================================================
   MESSAGE HANDLERS
======================================================== */
function onPublicMessageReceived(payload) {
  const message = JSON.parse(payload.body);
  if (currentChat?.type === 'public') displayMessage(message, true);
}
function onPrivateMessageReceived(payload) {
  const message = JSON.parse(payload.body);
  if (currentChat?.type === 'private' && currentChat?.id === message.senderId) {
    displayPrivateMessage(message, true);
  }
  updateChatListWithNewMessage(message);
}
function onGroupMessageReceived(payload) {
  const message = JSON.parse(payload.body);
  if (currentChat?.type === 'group' && currentChat?.id === message.groupId) {
    displayGroupMessage(message, true);
  }
  updateChatListWithNewMessage(message);
}
function onFriendRequestReceived(payload) {
  const notification = JSON.parse(payload.body);
  showFriendRequestNotification(notification);
  loadFriendsList();
}
function onUserStatusChanged() { loadFriendsList(); }

/* ========================================================
   DATA LOADING
======================================================== */
async function loadInitialData() {
  try { 
    await Promise.all([ 
      loadFriendsList(), 
      loadGroupsList(), 
      displayPrivateChatsList(), 
      loadChatHistory() 
    ]); 
  }
  catch (e) { console.error('Error loading initial data:', e); }
}

async function loadFriendsList() {
  try {
    const res = await fetch('/api/friends/list', { headers: { [csrfHeader]: csrfToken } });
    if (res.ok) displayFriendsList(await res.json());
  } catch (e) { console.error('Error loading friends:', e); }
}

async function loadGroupsList() {
  try {
    const res = await fetch('/api/groups/my-groups', { headers: { [csrfHeader]: csrfToken } });
    if (res.ok) displayGroupsList(await res.json());
  } catch (e) { console.error('Error loading groups:', e); }
}

async function loadChatHistory() {
  if (currentChat?.type === 'public') await loadPublicChatHistory();
  else if (currentChat?.type === 'private') await loadPrivateChatHistory(currentChat.id);
  else if (currentChat?.type === 'group') await loadGroupChatHistory(currentChat.id);
}

async function loadPublicChatHistory() {
  try {
    const res = await fetch('/api/messages/public');
    if (res.ok) {
      const messages = await res.json();
      chatMessages.innerHTML = '';
      messages.reverse().forEach(m => displayMessage(m, false));
      scrollToBottom();
    }
  } catch (e) { console.error('Error loading public chat history:', e); }
}

async function loadPrivateChatHistory(chatId) {
  try {
    const res = await fetch(`/api/private-chat/${chatId}/messages`, { headers: { [csrfHeader]: csrfToken } });
    if (res.ok) {
      const messages = await res.json();
      chatMessages.innerHTML = '';
      messages.forEach(m => displayPrivateMessage(m, false));
      scrollToBottom();
    }
  } catch (e) { console.error('Error loading private chat history:', e); }
}

async function loadGroupChatHistory(groupId) {
  try {
    const res = await fetch(`/api/groups/${groupId}/messages`, { headers: { [csrfHeader]: csrfToken } });
    if (res.ok) {
      const messages = await res.json();
      chatMessages.innerHTML = '';
      messages.forEach(m => displayGroupMessage(m, false));
      scrollToBottom();
    }
  } catch (e) { console.error('Error loading group chat history:', e); }
}

/* ========================================================
   STREAK FUNCTIONS
======================================================== */

async function loadPrivateChatStreak(chatId) {
  try {
    const res = await fetch(`/api/private-chat/${chatId}/streak`, { 
      headers: { [csrfHeader]: csrfToken } 
    });
    if (res.ok) {
      return await res.json();
    }
  } catch (e) { 
    console.error('Error loading private chat streak:', e); 
  }
  return null;
}

async function loadGroupChatStreak(groupId) {
  try {
    const res = await fetch(`/api/group-chat/${groupId}/streak`, { 
      headers: { [csrfHeader]: csrfToken } 
    });
    if (res.ok) {
      return await res.json();
    }
  } catch (e) { 
    console.error('Error loading group chat streak:', e); 
  }
  return null;
}

async function restorePrivateChatStreak(chatId) {
  try {
    const res = await fetch(`/api/private-chat/${chatId}/restore-streak`, {
      method: 'POST',
      headers: { [csrfHeader]: csrfToken }
    });
    if (res.ok) {
      const result = await res.json();
      showSuccessMessage(result.message || 'Đã khôi phục streak thành công!');
      return result;
    } else {
      const error = await res.json();
      showErrorMessage(error.error || 'Không thể khôi phục streak');
    }
  } catch (e) { 
    console.error('Error restoring private chat streak:', e); 
    showErrorMessage('Có lỗi xảy ra khi khôi phục streak');
  }
  return null;
}

async function restoreGroupChatStreak(groupId) {
  try {
    const res = await fetch(`/api/group-chat/${groupId}/restore-streak`, {
      method: 'POST',
      headers: { [csrfHeader]: csrfToken }
    });
    if (res.ok) {
      const result = await res.json();
      showSuccessMessage(result.message || 'Đã khôi phục streak thành công!');
      return result;
    } else {
      const error = await res.json();
      showErrorMessage(error.error || 'Không thể khôi phục streak');
    }
  } catch (e) { 
    console.error('Error restoring group chat streak:', e); 
    showErrorMessage('Có lỗi xảy ra khi khôi phục streak');
  }
  return null;
}

/* ========================================================
   FRIENDS / GROUP LIST RENDER (updated with streak)
======================================================== */
function displayFriendsList(friends) {
  const chatList = $("#chatList");
  if (!chatList) return;
  chatList.innerHTML = '';
  addPublicChannelToList();
  if (friends.length > 0) {
    const friendsSection = createSection("👥 Bạn bè", friends.length);
    chatList.appendChild(friendsSection);
    friends.forEach(f => chatList.appendChild(createFriendItem(f)));
  }
}

function displayGroupsList(groups) {
  const chatList = $("#chatList");
  if (!chatList || groups.length === 0) return;
  const groupsSection = createSection("🏠 Nhóm chat", groups.length);
  chatList.appendChild(groupsSection);
  groups.forEach(g => chatList.appendChild(createGroupItem(g)));
}

async function displayPrivateChatsList() {
  try {
    const res = await fetch('/api/private-chat/my-chats', { 
      headers: { [csrfHeader]: csrfToken } 
    });
    if (res.ok) {
      const chats = await res.json();
      const chatList = $("#chatList");
      if (!chatList) return;
      
      if (chats.length > 0) {
        const chatsSection = createSection("💬 Chat riêng", chats.length);
        chatList.appendChild(chatsSection);
        chats.forEach(chat => {
          const item = createPrivateChatItem(chat);
          chatList.appendChild(item);
        });
      }
    }
  } catch (e) { 
    console.error('Error loading private chats:', e); 
  }
}

function createPrivateChatItem(chat) {
  const displayName = chat.otherUser.fullName || chat.otherUser.username;
  const streakDisplay = chat.streak ? ` ${chat.streak}` : '';
  const initials = getInitials(displayName);
  const gradient = pickGradient(simpleHash(chat.otherUser.username||''));
  const w = document.createElement('div');
  w.className = "p-3 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors";
  w.onclick = () => switchToPrivateChatById(chat.chatId, chat.otherUser);
  w.innerHTML = `
    <div class="flex items-center space-x-3">
      <div class="relative">
        <div class="w-10 h-10 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center"><span class="text-white text-sm font-medium">${initials}</span></div>
        <div class="absolute -bottom-1 -right-1 w-3 h-3 ${chat.otherUser.status==='ONLINE'?'bg-green-500':'bg-gray-400'} rounded-full border-2 border-white dark:border-gray-900"></div>
      </div>
      <div class="flex-1 min-w-0">
        <h4 class="font-medium text-gray-900 dark:text-white truncate text-sm">${displayName}${streakDisplay}</h4>
        <p class="text-xs text-gray-500 dark:text-gray-400 truncate">${chat.otherUser.status==='ONLINE'?'Đang online':'Offline'}</p>
      </div>
    </div>`;
  return w;
}

function addPublicChannelToList() {
  const chatList = $("#chatList");
  const item = document.createElement('div');
  item.className = "p-3 rounded-xl bg-purple-100 dark:bg-purple-900/30 border-l-4 border-purple-500 cursor-pointer mb-2";
  item.onclick = () => switchToPublicChat();
  
  item.innerHTML = `
    <div class="flex items-center space-x-3">
      <div class="relative">
        <div class="w-12 h-12 bg-gradient-to-r from-purple-500 to-purple-700 rounded-full flex items-center justify-center"><span class="text-white font-bold">🌐</span></div>
        <div class="absolute -top-1 -right-1 w-5 h-5 bg-green-500 rounded-full border-2 border-white dark:border-gray-900"></div>
      </div>
      <div class="flex-1 min-w-0">
        <h3 class="font-semibold text-gray-900 dark:text-white truncate">Kênh chung</h3>
        <p class="text-sm text-gray-600 dark:text-gray-400 truncate">Phòng chat công khai</p>
      </div>
    </div>`;
  
  chatList.appendChild(item);
}
function createSection(title, count) {
  const s = document.createElement("div");
  s.className = "px-3 py-2 border-t border-gray-200 dark:border-gray-700 mt-2 first:mt-0 first:border-t-0";
  s.innerHTML = `<h4 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">${title} ${count>0?`(${count})`:''}</h4>`;
  return s;
}
function createFriendItem(friend) {
  // Ensure we always show full name if available, otherwise show username
  const displayName = (friend.fullName && friend.fullName.trim()) ? friend.fullName : friend.username;
  const initials = getInitials(displayName);
  const gradient = pickGradient(simpleHash(friend.username||''));
  const w = document.createElement('div');
  w.className = "p-3 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors";
  w.onclick = () => switchToPrivateChat(friend);
  w.innerHTML = `
    <div class="flex items-center space-x-3">
      <div class="relative">
        <div class="w-10 h-10 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center"><span class="text-white text-sm font-medium">${initials}</span></div>
        <div class="absolute -bottom-1 -right-1 w-3 h-3 ${friend.status==='ONLINE'?'bg-green-500':'bg-gray-400'} rounded-full border-2 border-white dark:border-gray-900"></div>
      </div>
      <div class="flex-1 min-w-0">
        <h4 class="font-medium text-gray-900 dark:text-white truncate text-sm">${displayName}</h4>
        <p class="text-xs text-gray-500 dark:text-gray-400 truncate">${friend.status==='ONLINE'?'Đang online':'Offline'}</p>
      </div>
    </div>`;
  return w;
}
function createGroupItem(group) {
  const initials = getInitials(group.name);
  const gradient = pickGradient(simpleHash(group.name||''));
  const w = document.createElement('div');
  w.className = "p-3 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors";
  w.onclick = () => switchToGroupChat(group);
  
  // Load streak for group asynchronously
  loadGroupChatStreak(group.id).then(streakData => {
    const streakDisplay = streakData && streakData.display ? ` ${streakData.display}` : '';
    w.innerHTML = `
      <div class="flex items-center space-x-3">
        <div class="relative">
          <div class="w-10 h-10 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center"><span class="text-white text-sm font-medium">${initials}</span></div>
          <div class="absolute -bottom-1 -right-1 w-3 h-3 bg-blue-500 rounded-full border-2 border-white dark:border-gray-900"></div>
        </div>
        <div class="flex-1 min-w-0">
          <h4 class="font-medium text-gray-900 dark:text-white truncate text-sm">${group.name}${streakDisplay}</h4>
          <p class="text-xs text-gray-500 dark:text-gray-400 truncate">${group.memberCount} thành viên</p>
        </div>
      </div>`;
  }).catch(() => {
    // Fallback if streak loading fails
    w.innerHTML = `
      <div class="flex items-center space-x-3">
        <div class="relative">
          <div class="w-10 h-10 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center"><span class="text-white text-sm font-medium">${initials}</span></div>
          <div class="absolute -bottom-1 -right-1 w-3 h-3 bg-blue-500 rounded-full border-2 border-white dark:border-gray-900"></div>
        </div>
        <div class="flex-1 min-w-0">
          <h4 class="font-medium text-gray-900 dark:text-white truncate text-sm">${group.name}</h4>
          <p class="text-xs text-gray-500 dark:text-gray-400 truncate">${group.memberCount} thành viên</p>
        </div>
      </div>`;
  });
  
  return w;
}

async function switchToPrivateChatById(chatId, otherUser) {
  currentChat = { type: 'private', id: chatId, name: otherUser.fullName || otherUser.username, friendId: otherUser.id };
  updateChatHeader(getInitials(currentChat.name), `Chat với ${currentChat.name}`, 'Chat riêng tư');
  await loadPrivateChatHistory(chatId);
}

/* ========================================================
   SWITCH CHAT
======================================================== */
function switchToPublicChat() {
  currentChat = { type: 'public', id: null, name: 'Kênh chung' };
  updateChatHeader('🌐', 'Kênh chung', 'Phòng chat công khai');
  loadPublicChatHistory();
}
async function switchToPrivateChat(friend) {
  try {
    const res = await fetch('/api/private-chat/start-chat', {
      method: 'POST', headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
      body: JSON.stringify({ username: friend.username })
    });
    if (res.ok) {
      const chatData = await res.json();
      currentChat = { type: 'private', id: chatData.chatId, name: friend.fullName || friend.username, friendId: friend.id };
      updateChatHeader(getInitials(currentChat.name), `Chat với ${currentChat.name}`, 'Chat riêng tư');
      await loadPrivateChatHistory(chatData.chatId);
    } else showErrorMessage('Không thể bắt đầu chat riêng');
  } catch (e) { console.error('Error starting private chat:', e); showErrorMessage('Có lỗi xảy ra khi mở chat riêng'); }
}
function switchToGroupChat(group) {
  currentChat = { type: 'group', id: group.id, name: group.name };
  updateChatHeader(getInitials(group.name), group.name, `${group.memberCount} thành viên`);
  loadGroupChatHistory(group.id);
}
function updateChatHeader(icon, title, subtitle) {
  const chatTitle = $("#chatTitle");
  const appStatusText = $("#appStatusText");
  
  // Load streak asynchronously and update title
  if (currentChat?.type === 'private' && currentChat?.id) {
    loadPrivateChatStreak(currentChat.id).then(streakData => {
      const streakDisplay = streakData && streakData.display ? ` ${streakData.display}` : '';
      if (chatTitle) chatTitle.textContent = title + streakDisplay;
    }).catch(() => {
      if (chatTitle) chatTitle.textContent = title;
    });
  } else if (currentChat?.type === 'group' && currentChat?.id) {
    loadGroupChatStreak(currentChat.id).then(streakData => {
      const streakDisplay = streakData && streakData.display ? ` ${streakData.display}` : '';
      if (chatTitle) chatTitle.textContent = title + streakDisplay;
    }).catch(() => {
      if (chatTitle) chatTitle.textContent = title;
    });
  } else {
    if (chatTitle) chatTitle.textContent = title;
  }
  
  if (appStatusText) appStatusText.textContent = subtitle;
}

/* ========================================================
   SEND MESSAGE
======================================================== */
function sendMessage(evt) {
  const content = messageInput?.value.trim();
  if (!content || !stompClient || messageInput.disabled) return;
  if (currentChat?.type === 'public') sendPublicMessage(content);
  else if (currentChat?.type === 'private') sendPrivateMessage(content);
  else if (currentChat?.type === 'group') sendGroupMessage(content);
  messageInput.value = '';
  evt?.preventDefault();
}
function sendPublicMessage(content) {
  const msg = { sender: username, content, type: 'CHAT', timestamp: new Date().toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'}) };
  stompClient.send('/app/chat.send', {}, JSON.stringify(msg));
  
  // Update public chat header with streak after sending
  setTimeout(() => {
    if (currentChat?.type === 'public') {
      updateChatHeader('🌐', 'Kênh chung', 'Phòng chat công khai');
      addPublicChannelToList(); // Refresh the public channel item
    }
  }, 1000);
}
async function sendPrivateMessage(content) {
  try {
    const res = await fetch(`/api/private-chat/${currentChat.id}/send`, {
      method:'POST', headers:{ 'Content-Type':'application/json', [csrfHeader]: csrfToken }, body: JSON.stringify({ content })
    });
    if (res.ok) {
      const messageData = await res.json();
      displayPrivateMessage(messageData, true);
      
      // Update chat header with new streak if provided
      if (messageData.streak) {
        const chatTitle = $("#chatTitle");
        if (chatTitle) {
          const baseName = `Chat với ${currentChat.name}`;
          chatTitle.textContent = baseName + (messageData.streak ? ` ${messageData.streak}` : '');
        }
      }
      
      // Refresh chat list to show updated streaks
      displayPrivateChatsList();
    }
  } catch (e) { console.error('Error sending private message:', e); }
}
async function sendGroupMessage(content) {
  try {
    const res = await fetch(`/api/groups/${currentChat.id}/send`, {
      method:'POST', headers:{ 'Content-Type':'application/json', [csrfHeader]: csrfToken }, body: JSON.stringify({ content })
    });
    if (res.ok) {
      const messageData = await res.json();
      displayGroupMessage(messageData, true);
      
      // Update chat header with new streak if available
      updateChatHeader(getInitials(currentChat.name), currentChat.name, `Nhóm chat`);
      
      // Refresh chat list to show updated streaks
      loadGroupsList();
    }
  } catch (e) { console.error('Error sending group message:', e); }
}

/* ========================================================
   MESSAGE DISPLAY
======================================================== */
function displayMessage(message, autoScroll = true) {
  const div = document.createElement('div');
  div.className = 'flex items-start space-x-3 message-bubble';
  const isMe = message.sender === username;
  const displayName = message.fullName || message.sender;
  const initials = getInitials(displayName);
  const gradient = pickGradient(simpleHash(message.sender||''));
  const time = message.timestamp || new Date().toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'});

  if (message.type === 'JOIN') {
    div.className = 'flex justify-center my-4';
    div.innerHTML = `<div class="glass-effect px-6 py-3 rounded-full text-sm text-green-600 dark:text-green-300">${displayName} đã tham gia phòng chat 👋</div>`;
  } else if (message.type === 'LEAVE') {
    div.className = 'flex justify-center my-4';
    div.innerHTML = `<div class="glass-effect px-6 py-3 rounded-full text-sm text-red-600 dark:text-red-300">${displayName} đã rời khỏi phòng chat 👋</div>`;
  } else if (isMe) {
    div.classList.add('justify-end');
    div.innerHTML = `
      <div class="bg-gradient-to-r from-purple-500 to-purple-700 rounded-2xl rounded-tr-md px-4 py-3 max-w-xs lg:max-w-md">
        <p class="text-white">${message.content}</p>
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-purple-100">${time}</span></div>
      </div>
      <div class="w-8 h-8 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center flex-shrink-0"><span class="text-white text-sm font-bold">${initials}</span></div>`;
  } else {
    div.classList.add('items-start');
    div.innerHTML = `
      <div class="w-8 h-8 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center flex-shrink-0"><span class="text-white text-sm font-bold">${initials}</span></div>
      <div class="bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-md px-4 py-3 max-w-xs lg:max-w-md">
        <div class="text-xs text-gray-500 dark:text-gray-400 mb-1 font-medium">${displayName}</div>
        <p class="text-gray-800 dark:text-gray-200">${message.content}</p>
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-gray-500 dark:text-gray-400">${time}</span></div>
      </div>`;
  }
  chatMessages?.appendChild(div);
  if (autoScroll) scrollToBottom();
}

function displayPrivateMessage(message, autoScroll = true) {
  const div = document.createElement('div');
  div.className = 'flex items-start space-x-3 message-bubble';
  const isMe = message.sender.username === username;
  const displayName = message.sender.fullName || message.sender.username;
  const initials = getInitials(displayName);
  const gradient = pickGradient(simpleHash(message.sender.username||''));
  const time = new Date(message.timestamp).toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'});

  if (isMe) {
    div.classList.add('justify-end');
    div.innerHTML = `
      <div class="bg-gradient-to-r from-purple-500 to-purple-700 rounded-2xl rounded-tr-md px-4 py-3 max-w-xs lg:max-w-md">
        <p class="text-white">${message.content}</p>
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-purple-100">${time}</span></div>
      </div>
      <div class="w-8 h-8 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center flex-shrink-0"><span class="text-white text-sm font-bold">${initials}</span></div>`;
  } else {
    div.classList.add('items-start');
    div.innerHTML = `
      <div class="w-8 h-8 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center flex-shrink-0"><span class="text-white text-sm font-bold">${initials}</span></div>
      <div class="bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-md px-4 py-3 max-w-xs lg:max-w-md">
        <div class="text-xs text-gray-500 dark:text-gray-400 mb-1 font-medium">${displayName}</div>
        <p class="text-gray-800 dark:text-gray-200">${message.content}</p>
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-gray-500 dark:text-gray-400">${time}</span></div>
      </div>`;
  }
  chatMessages?.appendChild(div);
  if (autoScroll) scrollToBottom();
}

function displayGroupMessage(message, autoScroll = true) {
  const div = document.createElement('div');
  div.className = 'flex items-start space-x-3 message-bubble';
  const isMe = message.sender.username === username;
  const displayName = message.sender.fullName || message.sender.username;
  const initials = getInitials(displayName);
  const gradient = pickGradient(simpleHash(message.sender.username||''));
  const time = new Date(message.timestamp).toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'});

  if (isMe) {
    div.classList.add('justify-end');
    div.innerHTML = `
      <div class="bg-gradient-to-r from-purple-500 to-purple-700 rounded-2xl rounded-tr-md px-4 py-3 max-w-xs lg:max-w-md">
        <p class="text-white">${message.content}</p>
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-purple-100">${time}</span></div>
      </div>
      <div class="w-8 h-8 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center flex-shrink-0"><span class="text-white text-sm font-bold">${initials}</span></div>`;
  } else {
    div.classList.add('items-start');
    div.innerHTML = `
      <div class="w-8 h-8 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center flex-shrink-0"><span class="text-white text-sm font-bold">${initials}</span></div>
      <div class="bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-md px-4 py-3 max-w-xs lg:max-w-md">
        <div class="text-xs text-gray-500 dark:text-gray-400 mb-1 font-medium">${displayName}</div>
        <p class="text-gray-800 dark:text-gray-200">${message.content}</p>
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-gray-500 dark:text-gray-400">${time}</span></div>
      </div>`;
  }
  chatMessages?.appendChild(div);
  if (autoScroll) scrollToBottom();
}

/* ========================================================
   FRIEND SYSTEM
======================================================== */


async function loadPendingFriendRequests() {
  try {
    const res = await fetch('/api/friends/pending-requests', { headers: { [csrfHeader]: csrfToken } });
    if (res.ok) { const list = await res.json(); updateFriendRequestsBadge(list.length); return list; }
  } catch (e) { console.error('Error loading pending friend requests:', e); }
  return [];
}
function updateFriendRequestsBadge(count) {
  const badge = $("#friendRequestBadge");
  if (!badge) return;
  if (count > 0) { badge.textContent = count; badge.classList.remove('hidden'); }
  else badge.classList.add('hidden');
}
async function showFriendRequestsModal() {
  const modal = $("#friendRequestsModal");
  const list = $("#friendRequestsList");
  const empty = $("#noFriendRequests");
  if (!modal || !list) return;
  modal.classList.remove('hidden');
  const requests = await loadPendingFriendRequests();
  list.innerHTML = '';
  if (requests.length === 0) list.appendChild(empty); else requests.forEach(r => list.appendChild(createFriendRequestItem(r)));
}
function createFriendRequestItem(request) {
  const displayName = request.fullName || request.username;
  const initials = getInitials(displayName);
  const gradient = pickGradient(simpleHash(request.username||''));
  const w = document.createElement('div');
  w.className = 'flex items-center space-x-3 p-3 bg-gray-50 dark:bg-gray-700 rounded-xl';
  w.innerHTML = `
    <div class="w-12 h-12 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center flex-shrink-0"><span class="text-white font-bold">${initials}</span></div>
    <div class="flex-1 min-w-0">
      <h4 class="font-medium text-gray-900 dark:text-white truncate">${displayName}</h4>
      <p class="text-sm text-gray-500 dark:text-gray-400">muốn kết bạn với bạn</p>
    </div>
    <div class="flex space-x-2">
      <button onclick="acceptFriendRequest(${request.id})" class="px-3 py-1 bg-green-500 hover:bg-green-600 text-white text-sm rounded-lg">Chấp nhận</button>
      <button onclick="rejectFriendRequest(${request.id})" class="px-3 py-1 bg-gray-500 hover:bg-gray-600 text-white text-sm rounded-lg">Từ chối</button>
    </div>`;
  return w;
}
function showFriendRequestNotification(n) {
  if (n.type === 'FRIEND_REQUEST_RECEIVED') {
    showNotificationToast(`${n.fromUser} muốn kết bạn với bạn`, 'info', () => showFriendRequestsModal());
    loadPendingFriendRequests(); loadFriendsList();
  } else if (n.type === 'FRIEND_REQUEST_ACCEPTED') {
    showNotificationToast(`${n.fromUser} đã chấp nhận lời mời kết bạn`, 'success');
    loadFriendsList();
  } else if (n.type === 'FRIEND_LIST_UPDATE') {
    loadFriendsList(); loadPendingFriendRequests();
  }
}
async function acceptFriendRequest(userId) {
  try {
    const res = await fetch(`/api/friends/accept/${userId}`, { method:'POST', headers:{ [csrfHeader]: csrfToken } });
    if (res.ok) { showSuccessMessage('Đã chấp nhận lời mời kết bạn'); showFriendRequestsModal(); loadFriendsList(); }
    else showErrorMessage(`Lỗi: ${await res.text()}`);
  } catch (e) { console.error('Error accepting friend request:', e); showErrorMessage('Có lỗi xảy ra'); }
}
async function rejectFriendRequest(userId) {
  try {
    const res = await fetch(`/api/friends/reject/${userId}`, { method:'POST', headers:{ [csrfHeader]: csrfToken } });
    if (res.ok) { showSuccessMessage('Đã từ chối lời mời kết bạn'); showFriendRequestsModal(); }
    else showErrorMessage(`Lỗi: ${await res.text()}`);
  } catch (e) { console.error('Error rejecting friend request:', e); showErrorMessage('Có lỗi xảy ra'); }
}

/* ========================================================
   TOASTS / UTILITIES (from your base)
======================================================== */
function showNotificationToast(message, type='info', onClick=null) {
  const toast = document.createElement('div');
  toast.className = `fixed top-4 right-4 max-w-sm bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg p-4 z-50 transform transition-all duration-300 translate-x-full`;
  const bg = type==='success'?'border-green-500': type==='error'?'border-red-500':'border-blue-500';
  toast.classList.add(bg);
  toast.innerHTML = `
    <div class="flex items-center justify-between">
      <div class="flex items-center space-x-3">
        <div class="text-2xl">${type==='success'?'✅': type==='error'?'❌':'📬'}</div>
        <div>
          <p class="text-sm font-medium text-gray-900 dark:text-white">${message}</p>
          ${onClick ? '<p class="text-xs text-gray-500 dark:text-gray-400 mt-1">Nhấn để xem chi tiết</p>' : ''}
        </div>
      </div>
      <button class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" onclick="this.parentElement.parentElement.remove()">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
      </button>
    </div>`;
  if (onClick) { toast.style.cursor='pointer'; toast.addEventListener('click', onClick); }
  document.body.appendChild(toast);
  setTimeout(()=>toast.classList.remove('translate-x-full'), 100);
  setTimeout(()=>{ toast.classList.add('translate-x-full'); setTimeout(()=>toast.remove(),300); }, 5000);
}
function scrollToBottom(){ if (chatMessages) chatMessages.scrollTop = chatMessages.scrollHeight; }
function showErrorMessage(m){ const d=document.createElement('div'); d.className='flex justify-center my-4'; d.innerHTML=`<div class="glass-effect px-6 py-3 rounded-full text-sm text-red-600 dark:text-red-300">${m}</div>`; chatMessages?.appendChild(d); scrollToBottom(); }
function showSuccessMessage(m){ const d=document.createElement('div'); d.className='flex justify-center my-4'; d.innerHTML=`<div class="glass-effect px-6 py-3 rounded-full text-sm text-green-600 dark:text-green-300">${m}</div>`; chatMessages?.appendChild(d); scrollToBottom(); }
function updateChatListWithNewMessage(){ loadFriendsList(); loadGroupsList(); }
function handleEnterKey(e){ if (e.key==='Enter' && !e.shiftKey){ e.preventDefault(); sendMessage(e); } }

/* ========================================================
   OPTIONAL UX ADD-ONS from file (auto-guarded by querySelector)
   - Chat Settings Drawer
   - Wallpaper presets / custom URL
   - Emoji picker
   - Big emoji toggle
   - Nickname override
   - Block/Mute toggles
   - Clear chat
   - Profile editor + Avatar (localStorage)
======================================================== */
// --- Chat Settings Drawer ---
const chatSettingsBtn   = $("#chatSettingsBtn");
const chatSettingsPanel = $("#chatSettingsPanel");
const chatSettingsOverlay = $("#chatSettingsOverlay");
const closeChatSettings = $("#closeChatSettings");
const mainChatArea      = $("#mainChatArea");
function openChatSettings(){ chatSettingsPanel?.classList.remove('translate-x-full'); chatSettingsOverlay?.classList.remove('hidden'); mainChatArea?.classList.add('mr-80'); }
function closeChatSettingsPanel(){ chatSettingsPanel?.classList.add('translate-x-full'); chatSettingsOverlay?.classList.add('hidden'); mainChatArea?.classList.remove('mr-80'); }
chatSettingsBtn   ?.addEventListener('click', openChatSettings);
closeChatSettings ?.addEventListener('click', closeChatSettingsPanel);
chatSettingsOverlay?.addEventListener('click', closeChatSettingsPanel);
window.addEventListener('keydown', (e)=>{ if (e.key==='Escape') closeChatSettingsPanel(); });

// --- Wallpaper ---
const wpPresetsEl  = $("#wpPresets");
const wpUrlInput   = $("#wpUrl");
const applyWpUrlBtn= $("#applyWpUrl");
const resetWpBtn   = $("#resetWp");
const WP_PRESETS = { none:{type:'preset',key:'none'}, 'grad-purple':{type:'preset',key:'grad-purple'}, 'grad-blue':{type:'preset',key:'grad-blue'}, 'grad-pink':{type:'preset',key:'grad-pink'}, dots:{type:'preset',key:'dots'}, grid:{type:'preset',key:'grid'} };
function applyWallpaperStyle(p){ if (!chatMessages) return; chatMessages.style.backgroundImage='none'; chatMessages.style.backgroundSize=''; chatMessages.style.backgroundPosition=''; chatMessages.style.backgroundAttachment=''; chatMessages.style.backgroundColor=''; if (!p) return; if (p.type==='url'){ chatMessages.style.backgroundImage=`url("${p.url}")`; chatMessages.style.backgroundSize='cover'; chatMessages.style.backgroundPosition='center'; chatMessages.style.backgroundAttachment='fixed'; return; } switch(p.key){ case 'grad-purple': chatMessages.style.backgroundImage='linear-gradient(135deg,#f5e1ff,#e7d4ff)'; break; case 'grad-blue': chatMessages.style.backgroundImage='linear-gradient(135deg,#dbeafe,#bfdbfe)'; break; case 'grad-pink': chatMessages.style.backgroundImage='linear-gradient(135deg,#ffe4e6,#fecdd3)'; break; case 'dots': chatMessages.style.backgroundImage='radial-gradient(#e5e7eb 1.2px, transparent 1.2px), radial-gradient(#e5e7eb 1.2px, transparent 1.2px)'; chatMessages.style.backgroundSize='20px 20px,20px 20px'; chatMessages.style.backgroundPosition='0 0,10px 10px'; chatMessages.style.backgroundColor='#ffffff'; break; case 'grid': chatMessages.style.backgroundImage='linear-gradient(rgba(0,0,0,.06) 1px, transparent 1px), linear-gradient(90deg, rgba(0,0,0,.06) 1px, transparent 1px)'; chatMessages.style.backgroundSize='24px 24px,24px 24px'; chatMessages.style.backgroundColor='#ffffff'; break; default: break; } }
function setActivePresetButton(key){ document.querySelectorAll('#wpPresets .wp-item').forEach(btn=>{ btn.setAttribute('aria-pressed', btn.getAttribute('data-wp')===key ? 'true':'false'); }); }
function saveWallpaper(d){ localStorage.setItem('chatWallpaper', JSON.stringify(d)); }
function loadWallpaper(){ try{return JSON.parse(localStorage.getItem('chatWallpaper')||'null');}catch{return null;} }
(function initWallpaper(){ const saved = loadWallpaper(); if (saved){ applyWallpaperStyle(saved); if (saved.type==='preset') setActivePresetButton(saved.key); else setActivePresetButton(''); } else { setActivePresetButton('none'); } })();
wpPresetsEl?.addEventListener('click', (e)=>{ const target = e.target.closest('.wp-item'); if (!target) return; const key = target.getAttribute('data-wp'); const data = WP_PRESETS[key] || WP_PRESETS.none; applyWallpaperStyle(data); setActivePresetButton(key); saveWallpaper(data); });
applyWpUrlBtn?.addEventListener('click', ()=>{ const url=(wpUrlInput?.value||'').trim(); if (!url) return; const data={type:'url',url}; applyWallpaperStyle(data); setActivePresetButton(''); saveWallpaper(data); });
resetWpBtn?.addEventListener('click', ()=>{ const data=WP_PRESETS.none; applyWallpaperStyle(data); setActivePresetButton('none'); saveWallpaper(data); if (wpUrlInput) wpUrlInput.value=''; });

// --- Big Emoji ---
const bigEmojiToggle = $("#bigEmojiToggle");
(function initBigEmoji(){ const v = localStorage.getItem('bigEmoji')==='1'; if (bigEmojiToggle) bigEmojiToggle.checked=v; document.body.classList.toggle('big-emoji', v); })();
bigEmojiToggle?.addEventListener('change', (e)=>{ const v=e.target.checked; localStorage.setItem('bigEmoji', v?'1':'0'); document.body.classList.toggle('big-emoji', v); });

// --- Nickname ---
const chatTitleEl = $("#chatTitle");
const chatNicknameInput = $("#chatNicknameInput");
const applyNickname = $("#applyNickname");
(function initNickname(){ const nick=localStorage.getItem('chatNickname'); if (nick && chatTitleEl && chatNicknameInput){ chatTitleEl.textContent=nick; chatNicknameInput.value=nick; } })();
applyNickname?.addEventListener('click', ()=>{ const nick=(chatNicknameInput?.value||'').trim(); if (nick){ if (chatTitleEl) chatTitleEl.textContent=nick; localStorage.setItem('chatNickname', nick); } else { if (chatTitleEl) chatTitleEl.textContent='WebChat Pro'; localStorage.removeItem('chatNickname'); } });

// --- Block / Mute ---
const blockToggle = $("#blockToggle");
function applyBlockState(v){ if (!messageInput || !sendButton) return; messageInput.disabled=v; sendButton.disabled=v; messageInput.classList.toggle('opacity-60',v); sendButton.classList.toggle('opacity-60',v); messageInput.classList.toggle('cursor-not-allowed',v); sendButton.classList.toggle('cursor-not-allowed',v); }
(function initBlock(){ const blocked = localStorage.getItem('chatBlocked')==='1'; if (blockToggle) blockToggle.checked=blocked; applyBlockState(blocked); })();
blockToggle?.addEventListener('change',(e)=>{ const v=e.target.checked; localStorage.setItem('chatBlocked', v?'1':'0'); applyBlockState(v); });
const muteToggle = $("#muteToggle");
(function initMute(){ const muted=localStorage.getItem('chatMuted')==='1'; if (muteToggle) muteToggle.checked=muted; })();
muteToggle?.addEventListener('change',(e)=>{ const v=e.target.checked; localStorage.setItem('chatMuted', v?'1':'0'); });

// --- Clear chat ---
const typingIndicator = $("#typingIndicator");
const clearChatBtn = $("#clearChatBtn");
clearChatBtn?.addEventListener('click', ()=>{ if (!chatMessages || !typingIndicator) return; Array.from(chatMessages.children).forEach(n=>{ if(n!==typingIndicator) n.remove(); }); const info=document.createElement('div'); info.className='flex justify-center'; info.innerHTML='<div class="glass-effect px-6 py-3 rounded-full text-sm text-gray-600 dark:text-gray-300">Đoạn chat đã được xóa 🗑️</div>'; chatMessages.appendChild(info); closeChatSettingsPanel(); });

// --- Emoji Picker ---
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
function getRecentEmojis(){ try{return JSON.parse(localStorage.getItem('recentEmojis')||'[]');}catch{return [];} }
function saveRecentEmoji(e){ let arr=getRecentEmojis().filter(x=>x!==e); arr.unshift(e); if (arr.length>24) arr=arr.slice(0,24); localStorage.setItem('recentEmojis', JSON.stringify(arr)); }
function renderEmojiGrid(cat='recent'){ if (!emojiGrid) return; let list=[]; if (cat==='recent') list=getRecentEmojis(); if (!list || list.length===0){ cat = cat==='recent' ? 'camxuc' : cat; list = EMOJI_CATEGORIES[cat] || []; } emojiGrid.innerHTML=''; list.forEach(e=>{ const btn=document.createElement('button'); btn.type='button'; btn.className='emoji-btn'; btn.textContent=e; btn.addEventListener('click', ()=>{ insertAtCursor(messageInput, e); saveRecentEmoji(e); messageInput?.focus(); }); emojiGrid.appendChild(btn); }); }
function setActiveTab(cat){ document.querySelectorAll('#emojiPicker .emoji-tab').forEach(t=>{ t.setAttribute('aria-selected', t.getAttribute('data-cat')===cat ? 'true':'false'); }); renderEmojiGrid(cat); }
function insertAtCursor(input, text){ if (!input) return; const start=input.selectionStart ?? input.value.length; const end=input.selectionEnd ?? input.value.length; const before=input.value.slice(0,start); const after=input.value.slice(end); input.value = before + text + after; const newPos = start + text.length; input.setSelectionRange(newPos, newPos); }
function openEmojiPicker(){ if (!emojiPicker || !emojiToggleBtn) return; emojiPicker.classList.remove('hidden'); emojiToggleBtn.setAttribute('aria-expanded','true'); setActiveTab('recent'); }
function closeEmojiPicker(){ if (!emojiPicker || !emojiToggleBtn) return; emojiPicker.classList.add('hidden'); emojiToggleBtn.setAttribute('aria-expanded','false'); }
emojiToggleBtn?.addEventListener('click', (e)=>{ e.stopPropagation(); if (emojiPicker?.classList.contains('hidden')) openEmojiPicker(); else closeEmojiPicker(); });
emojiPicker?.addEventListener('click',(e)=>{ const tab=e.target.closest('.emoji-tab'); if (tab) setActiveTab(tab.getAttribute('data-cat')); });
document.addEventListener('click', (e)=>{ if (!emojiPicker || !emojiToggleBtn) return; if (!emojiPicker.classList.contains('hidden')){ if (!emojiPicker.contains(e.target) && e.target!==emojiToggleBtn) closeEmojiPicker(); } });
window.addEventListener('keydown',(e)=>{ if (e.key==='Escape') closeEmojiPicker(); });
renderEmojiGrid('recent');

// --- Profile / Avatar localStorage ---
const profileNameEl    = $("#profileName");
const profileStatusEl  = $("#profileStatus");
const editProfileBtn   = $("#editProfileBtn");
const editNameInput    = $("#editName");
const editStatusSelect = $("#editStatus");
const editPersistChk   = $("#editPersist");
const appStatusText    = $("#appStatusText");
const appStatusDot     = $("#appStatusDot");
const profileDot       = $("#profileDot");
const STATUS_MAP = { active:{label:'Đang hoạt động', colorClass:'bg-green-500'}, busy:{label:'Đang bận', colorClass:'bg-yellow-500'}, offline:{label:'Tắt trạng thái hoạt động', colorClass:'bg-red-500'} };
const STATUS_KEYS = Object.keys(STATUS_MAP);
const ALL_DOT_CLASSES = ['bg-green-500','bg-yellow-500','bg-red-500'];
function labelToKey(label=''){ label=(label||'').toLowerCase().trim(); if (label.includes('bận')) return 'busy'; if (label.includes('tắt')) return 'offline'; return 'active'; }
function updateDotsColor(dotEl, key){ if (!dotEl) return; dotEl.classList.remove(...ALL_DOT_CLASSES); dotEl.classList.add(STATUS_MAP[key].colorClass); }
function updateStatusUI(key){ const {label}=STATUS_MAP[key]||STATUS_MAP.active; if (profileStatusEl) profileStatusEl.textContent=label; if (appStatusText) appStatusText.textContent=label; updateDotsColor(profileDot,key); updateDotsColor(appStatusDot,key); }
(function initProfileFromStorage(){ const nameLS=localStorage.getItem('profileName'); const statusKeyLS=localStorage.getItem('profileStatusKey'); const statusTextLS=localStorage.getItem('profileStatus'); if (nameLS && profileNameEl) profileNameEl.textContent=nameLS; let initKey='active'; if (statusKeyLS && STATUS_MAP[statusKeyLS]) initKey=statusKeyLS; else if (statusTextLS) initKey=labelToKey(statusTextLS); updateStatusUI(initKey); if (editStatusSelect) editStatusSelect.value=initKey; })();
const profileAvatarImg      = $("#profileAvatarImg");
const profileAvatarFallback = $("#profileAvatarFallback");
const editAvatarPreview     = $("#editAvatarPreview");
const editAvatarFallback    = $("#editAvatarFallback");
const editAvatarBtn         = $("#editAvatarBtn");
const editAvatarFile        = $("#editAvatarFile");
const removeAvatarBtn       = $("#removeAvatarBtn");
function applyAvatar(dataUrl){ if (profileAvatarImg && profileAvatarFallback){ if (dataUrl){ profileAvatarImg.src=dataUrl; profileAvatarImg.style.display='block'; profileAvatarImg.classList.add('w-12','h-12'); profileAvatarFallback.style.display='none'; } else { profileAvatarImg.removeAttribute('src'); profileAvatarImg.style.display='none'; profileAvatarFallback.style.display='flex'; } } if (editAvatarPreview && editAvatarFallback){ if (dataUrl){ editAvatarPreview.src=dataUrl; editAvatarPreview.style.display='block'; editAvatarFallback.style.display='none'; } else { editAvatarPreview.removeAttribute('src'); editAvatarPreview.style.display='none'; editAvatarFallback.style.display='flex'; } } }
function fileToDataURL(file){ return new Promise((resolve,reject)=>{ const reader=new FileReader(); reader.onload=()=>resolve(reader.result); reader.onerror=reject; reader.readAsDataURL(file); }); }
(function initAvatarFromStorage(){ const saved=localStorage.getItem('profileAvatar')||''; applyAvatar(saved||null); })();
editProfileBtn?.addEventListener('click', ()=>{ const currentName=localStorage.getItem('profileName')||(profileNameEl?.textContent?.trim()||'Bạn'); const currentKey=localStorage.getItem('profileStatusKey')||labelToKey(profileStatusEl?.textContent?.trim()||''); if (editNameInput) editNameInput.value=currentName; if (editStatusSelect) editStatusSelect.value=STATUS_KEYS.includes(currentKey)?currentKey:'active'; applyAvatar(localStorage.getItem('profileAvatar')||null); if (editPersistChk){ editPersistChk.checked = Boolean(localStorage.getItem('profileName')||localStorage.getItem('profileStatusKey')||localStorage.getItem('profileStatus')); }
  const modal = document.getElementById('editProfileModal'); modal?.classList.remove('hidden'); });
$("#saveProfileChanges")?.addEventListener('click', async ()=>{
  const newName = (editNameInput?.value.trim() || 'Bạn');
  const key = editStatusSelect?.value || 'active';
  const safeKey = STATUS_KEYS.includes(key) ? key : 'active';
  
  try {
    // Call API to update profile in database
    const response = await fetch('/api/users/profile', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        [csrfHeader]: csrfToken
      },
      body: JSON.stringify({
        fullName: newName,
        status: safeKey
      })
    });

    if (response.ok) {
      const result = await response.json();
      console.log('Profile updated successfully:', result);
      
      // Update UI
      if (profileNameEl) profileNameEl.textContent = newName;
      updateStatusUI(safeKey);
      
      // Still save to localStorage if checkbox is checked
      if (editPersistChk?.checked) {
        localStorage.setItem('profileName', newName);
        localStorage.setItem('profileStatusKey', safeKey);
        localStorage.setItem('profileStatus', STATUS_MAP[safeKey].label);
      } else {
        localStorage.removeItem('profileName');
        localStorage.removeItem('profileStatusKey');
        localStorage.removeItem('profileStatus');
      }
      
      // Show success message
      showSuccessMessage('Cập nhật hồ sơ thành công!');
    } else {
      const error = await response.json();
      console.error('Failed to update profile:', error);
      showErrorMessage('Không thể cập nhật hồ sơ: ' + (error.message || 'Lỗi không xác định'));
      return;
    }
  } catch (error) {
    console.error('Error updating profile:', error);
    showErrorMessage('Lỗi kết nối: Không thể cập nhật hồ sơ');
    return;
  }
  
  document.getElementById('editProfileModal')?.classList.add('hidden');
});

// Load profile from database
async function loadProfileFromDatabase() {
  try {
    const response = await fetch('/api/users/profile', {
      method: 'GET',
      headers: {
        [csrfHeader]: csrfToken
      }
    });

    if (response.ok) {
      const profile = await response.json();
      console.log('Profile loaded from database:', profile);
      
      // Update UI with database values
      if (profileNameEl && profile.fullName) {
        profileNameEl.textContent = profile.fullName;
      }
      
      if (profile.status) {
        updateStatusUI(profile.status);
      }
      
      // Only override localStorage if no local data exists
      if (!localStorage.getItem('profileName') && profile.fullName) {
        localStorage.setItem('profileName', profile.fullName);
      }
      if (!localStorage.getItem('profileStatusKey') && profile.status) {
        localStorage.setItem('profileStatusKey', profile.status);
        localStorage.setItem('profileStatus', STATUS_MAP[profile.status]?.label || 'Đang hoạt động');
      }
      
    } else {
      console.error('Failed to load profile from database');
    }
  } catch (error) {
    console.error('Error loading profile from database:', error);
  }
}
editAvatarBtn?.addEventListener('click', ()=> editAvatarFile?.click());
editAvatarFile?.addEventListener('change', async (e)=>{ const f=e.target.files?.[0]; if (!f) return; if (f.size > 2*1200*1080){ alert('Ảnh quá lớn. Vui lòng chọn ảnh nhỏ hơn.'); editAvatarFile.value=''; return; } try{ const dataUrl=await fileToDataURL(f); localStorage.setItem('profileAvatar', dataUrl); applyAvatar(dataUrl); } catch(err){ console.error(err); alert('Không đọc được file ảnh.'); } });
removeAvatarBtn?.addEventListener('click', ()=>{ localStorage.removeItem('profileAvatar'); if (editAvatarFile) editAvatarFile.value=''; applyAvatar(null); });

/* ========================================================
   EVENTS
======================================================== */
document.addEventListener('DOMContentLoaded', function(){
  // Auto connect if username exists
  const usernameInput = $('#username');
  if (usernameInput && usernameInput.value){ 
    username = usernameInput.value.trim(); 
    console.log('🔄 Auto-connecting WebSocket for user:', username); 
    connect(); 
    
    // Load profile from database
    loadProfileFromDatabase();
  }

  $('#connectForm')?.addEventListener('submit', connect);
  sendButton?.addEventListener('click', sendMessage);
  messageInput?.addEventListener('keypress', handleEnterKey);

  // Add friend / create group buttons (keep your original behaviors)
  $('#addFriendBtn')?.addEventListener('click', showAddFriendDialog);
  $('#createGroupBtn')?.addEventListener('click', showCreateGroupDialog);


  // Search chats (basic)
  const searchInput = document.querySelector('input[placeholder="Tìm kiếm cuộc trò chuyện..."]');
  if (searchInput){
    searchInput.addEventListener('input', function(e){
      const s = e.target.value.toLowerCase().trim();
      const chatList = document.getElementById('chatList'); if (!chatList) return;
      chatList.querySelectorAll('[onclick]').forEach(item=>{
        const nameEl = item.querySelector('h4, h3');
        if (nameEl){ const name = nameEl.textContent.toLowerCase(); item.style.display = (s===''||name.includes(s)) ? '' : 'none'; }
      });
    });
  }

  // Pending friend requests badge
  setTimeout(()=>{ if (username) loadPendingFriendRequests(); }, 1000);

  // Ensure theme icons consistent on load
  updateThemeIcons();
});

console.log('🚀 WebChat Pro (WS + Friends/Groups + UI add-ons) initialized!');