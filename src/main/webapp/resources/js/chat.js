(() => {
    const root = window.location.pathname.substring(0, window.location.pathname.lastIndexOf('/'));
    const api = root + '/api/v1/chats';
    const status = document.getElementById('chatStatus');
    const chatList = document.getElementById('chatList');
    const chatCount = document.getElementById('chatCount');
    const userSelect = document.getElementById('chatUser');
    const emptyState = document.getElementById('chatEmpty');
    const activeChat = document.getElementById('chatActive');
    const messageList = document.getElementById('messageList');
    const messageForm = document.getElementById('messageForm');
    const messageInput = document.getElementById('messageInput');
    let conversations = [];
    let selectedConversationId = null;
    let refreshGeneration = 0;
    let pollTimer = null;

    async function request(url, options = {}) {
        const response = await fetch(url, {
            credentials: 'same-origin',
            ...options,
            headers: {'Content-Type': 'application/json', ...(options.headers || {})}
        });
        if (!response.ok) {
            let detail = '';
            try {
                const body = await response.json();
                detail = body.detail || body.message || '';
            } catch (_) {
                detail = '';
            }
            throw new Error(detail || 'Não foi possível concluir a solicitação.');
        }
        return response.status === 204 ? null : response.json();
    }

    function showError(error) {
        status.textContent = error.message;
    }

    function formatDate(value) {
        if (!value) return '';
        const date = new Date(value);
        return Number.isNaN(date.getTime())
            ? ''
            : new Intl.DateTimeFormat('pt-BR', {dateStyle: 'short', timeStyle: 'short'}).format(date);
    }

    function createChatButton(conversation) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'chat-list-item';
        button.setAttribute('aria-current', String(conversation.id === selectedConversationId));

        const name = document.createElement('span');
        name.className = 'chat-list-name';
        name.textContent = conversation.outroUsuarioNome || conversation.outroUsuarioEmail;
        const preview = document.createElement('span');
        preview.className = 'chat-list-preview';
        preview.textContent = conversation.ultimaMensagem || 'Conversa iniciada';
        button.append(name, preview);
        button.addEventListener('click', () => selectConversation(conversation.id));
        return button;
    }

    function renderConversations() {
        chatCount.textContent = String(conversations.length);
        chatList.replaceChildren();
        if (conversations.length === 0) {
            const message = document.createElement('p');
            message.className = 'chat-list-empty';
            message.textContent = 'Nenhuma conversa aberta. Inicie uma nova abaixo.';
            chatList.append(message);
            return;
        }
        conversations.forEach(conversation => chatList.append(createChatButton(conversation)));
    }

    async function loadConversations() {
        conversations = await request(api);
        renderConversations();
        if (selectedConversationId && !conversations.some(item => item.id === selectedConversationId)) {
            selectedConversationId = null;
            activeChat.hidden = true;
            emptyState.hidden = false;
        }
    }

    function renderMessages(messages, forceScrollToBottom = false) {
        const keepFollowing = forceScrollToBottom
            || messageList.scrollHeight - messageList.scrollTop - messageList.clientHeight < 60;
        messageList.replaceChildren();
        if (messages.length === 0) {
            const message = document.createElement('p');
            message.className = 'message-empty';
            message.textContent = 'Envie a primeira mensagem para iniciar a conversa.';
            messageList.append(message);
            return;
        }
        messages.forEach(item => {
            const bubble = document.createElement('article');
            bubble.className = item.minhaMensagem ? 'message message-own' : 'message';
            if (!item.minhaMensagem) {
                const sender = document.createElement('span');
                sender.className = 'message-sender';
                sender.textContent = item.remetenteNome;
                bubble.append(sender);
            }
            const content = document.createElement('p');
            content.className = 'message-content';
            content.textContent = item.conteudo;
            const time = document.createElement('time');
            time.className = 'message-time';
            time.textContent = formatDate(item.enviadaEm);
            bubble.append(content, time);
            messageList.append(bubble);
        });
        if (keepFollowing) messageList.scrollTop = messageList.scrollHeight;
    }

    async function loadMessages(conversationId, generation = refreshGeneration, forceScrollToBottom = false) {
        const messages = await request(api + '/' + conversationId + '/messages');
        if (generation === refreshGeneration && selectedConversationId === conversationId) {
            renderMessages(messages, forceScrollToBottom);
        }
    }

    async function selectConversation(conversationId) {
        selectedConversationId = conversationId;
        refreshGeneration++;
        const generation = refreshGeneration;
        const conversation = conversations.find(item => item.id === conversationId);
        if (!conversation) return;

        document.getElementById('contactName').textContent =
            conversation.outroUsuarioNome || conversation.outroUsuarioEmail;
        document.getElementById('contactEmail').textContent = conversation.outroUsuarioEmail;
        document.getElementById('contactAvatar').textContent =
            (conversation.outroUsuarioNome || conversation.outroUsuarioEmail).trim().charAt(0).toUpperCase();
        emptyState.hidden = true;
        activeChat.hidden = false;
        renderConversations();
        try {
            await loadMessages(conversationId, generation, true);
        } catch (error) {
            showError(error);
        }
        if (pollTimer) window.clearInterval(pollTimer);
        pollTimer = window.setInterval(async () => {
            try {
                await loadMessages(conversationId, generation);
                await loadConversations();
            } catch (error) {
                showError(error);
            }
        }, 5000);
    }

    async function loadUsers() {
        const users = await request(api + '/users');
        userSelect.replaceChildren();
        const placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = users.length ? 'Selecione um usuário' : 'Nenhum usuário disponível';
        userSelect.append(placeholder);
        users.forEach(user => {
            const option = document.createElement('option');
            option.value = String(user.id);
            option.textContent = user.nome + ' (' + user.email + ')';
            userSelect.append(option);
        });
        userSelect.disabled = users.length === 0;
    }

    document.getElementById('newChatForm').addEventListener('submit', async event => {
        event.preventDefault();
        if (!userSelect.value) return;
        status.textContent = '';
        try {
            const conversation = await request(api, {
                method: 'POST',
                body: JSON.stringify({usuarioId: Number(userSelect.value)})
            });
            await loadConversations();
            await selectConversation(conversation.id);
            userSelect.value = '';
        } catch (error) {
            showError(error);
        }
    });

    messageForm.addEventListener('submit', async event => {
        event.preventDefault();
        const content = messageInput.value.trim();
        if (!selectedConversationId || !content) return;
        const submit = messageForm.querySelector('button[type="submit"]');
        submit.disabled = true;
        status.textContent = '';
        try {
            await request(api + '/' + selectedConversationId + '/messages', {
                method: 'POST',
                body: JSON.stringify({conteudo: content})
            });
            messageInput.value = '';
            await loadMessages(selectedConversationId, refreshGeneration, true);
            await loadConversations();
        } catch (error) {
            showError(error);
        } finally {
            submit.disabled = false;
            messageInput.focus();
        }
    });

    Promise.all([loadConversations(), loadUsers()]).catch(showError);
})();
