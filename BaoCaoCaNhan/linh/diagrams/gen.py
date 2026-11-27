#!/usr/bin/env python3
"""Generate UML SVG diagrams for Linh's personal report (F01 Tài khoản, F02 Kết nối)."""
import html, os
HERE = os.path.dirname(os.path.abspath(__file__))
FONT = 'Times New Roman'


def esc(s):
    return html.escape(s, quote=False)


class SVG:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.items = []

    def add(self, s):
        self.items.append(s)

    def text(self, x, y, s, size=16, anchor='middle', weight='normal', style='normal', fill='#000'):
        self.add('<text x="%g" y="%g" font-family="%s" font-size="%g" text-anchor="%s" font-weight="%s" font-style="%s" fill="%s">%s</text>'
                 % (x, y, FONT, size, anchor, weight, style, fill, esc(s)))

    def line(self, x1, y1, x2, y2, dash=False, w=1.4, color='#000', marker=None):
        d = ' stroke-dasharray="7,5"' if dash else ''
        m = ' marker-end="url(#%s)"' % marker if marker else ''
        self.add('<line x1="%g" y1="%g" x2="%g" y2="%g" stroke="%s" stroke-width="%g"%s%s/>' % (x1, y1, x2, y2, color, w, d, m))

    def rect(self, x, y, w, h, fill='#fff', stroke='#000', sw=1.4, rx=0, dash=False):
        d = ' stroke-dasharray="6,4"' if dash else ''
        self.add('<rect x="%g" y="%g" width="%g" height="%g" rx="%g" fill="%s" stroke="%s" stroke-width="%g"%s/>' % (x, y, w, h, rx, fill, stroke, sw, d))

    def btext(self, x, y, s, size=15.5, anchor='middle', style='normal'):
        w = len(s) * size * 0.52 + 8
        x0 = x - w / 2 if anchor == 'middle' else (x - 3 if anchor == 'start' else x - w + 3)
        self.add('<rect x="%g" y="%g" width="%g" height="%g" fill="#fff"/>' % (x0, y - size + 2, w, size + 3))
        self.text(x, y, s, size, anchor=anchor, style=style)

    def save(self, name):
        defs = ('<defs>'
                '<marker id="open" markerWidth="12" markerHeight="10" refX="11" refY="5" orient="auto"><path d="M0,0 L11,5 L0,10" fill="none" stroke="#000" stroke-width="1.4"/></marker>'
                '<marker id="fill" markerWidth="12" markerHeight="10" refX="11" refY="5" orient="auto"><path d="M0,0 L11,5 L0,10 z" fill="#000"/></marker>'
                '<marker id="tri" markerWidth="16" markerHeight="14" refX="15" refY="7" orient="auto"><path d="M1,1 L15,7 L1,13 z" fill="#fff" stroke="#000" stroke-width="1.4"/></marker>'
                '<marker id="diamond" markerWidth="18" markerHeight="12" refX="17" refY="6" orient="auto"><path d="M1,6 L9,1 L17,6 L9,11 z" fill="#fff" stroke="#000" stroke-width="1.3"/></marker>'
                '</defs>')
        s = ('<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">%s'
             '<rect x="0" y="0" width="%d" height="%d" fill="#ffffff"/>%s</svg>'
             % (self.w, self.h, self.w, self.h, defs, self.w, self.h, ''.join(self.items)))
        open(os.path.join(HERE, name), 'w', encoding='utf8').write(s)


def actor(svg, x, y, label):
    svg.add('<circle cx="%g" cy="%g" r="13" fill="#fff" stroke="#000" stroke-width="1.6"/>' % (x, y))
    svg.line(x, y + 13, x, y + 50, w=1.6)
    svg.line(x - 22, y + 26, x + 22, y + 26, w=1.6)
    svg.line(x, y + 50, x - 18, y + 78, w=1.6)
    svg.line(x, y + 50, x + 18, y + 78, w=1.6)
    svg.text(x, y + 100, label, 17, weight='bold')


def ellipse(svg, cx, cy, rx, ry, lines, fill='#EAF1FB'):
    svg.add('<ellipse cx="%g" cy="%g" rx="%g" ry="%g" fill="%s" stroke="#1F3B63" stroke-width="1.5"/>' % (cx, cy, rx, ry, fill))
    n = len(lines)
    for i, l in enumerate(lines):
        svg.text(cx, cy + 6 + (i - (n - 1) / 2) * 19, l, 16)


def edge_point(cx, cy, rx, ry, tx, ty):
    import math
    dx, dy = tx - cx, ty - cy
    t = 1 / math.sqrt((dx / rx) ** 2 + (dy / ry) ** 2)
    return cx + dx * t, cy + dy * t


def dep(svg, a, b, label):
    (ax, ay, arx, ary), (bx, by, brx, bry) = a, b
    x1, y1 = edge_point(ax, ay, arx, ary, bx, by)
    x2, y2 = edge_point(bx, by, brx, bry, ax, ay)
    svg.line(x1, y1, x2, y2, dash=True, marker='open')
    mx, my = (x1 + x2) / 2, (y1 + y2) / 2
    if abs(x2 - x1) + abs(y2 - y1) < 150:
        svg.text(mx, my - 10, label, 14, style='italic'); return
    svg.add('<rect x="%g" y="%g" width="%g" height="20" fill="#fff"/>' % (mx - 42, my - 15, 84))
    svg.text(mx, my, label, 14, style='italic')


# ---------------------------------------------------------------- use case
def usecase():
    s = SVG(1040, 560)
    s.rect(175, 15, 850, 530, fill='#FBFCFE', rx=6)
    s.text(600, 42, 'Hệ thống Darts Battle – Tài khoản và Kết nối', 18, weight='bold')
    actor(s, 75, 120, 'Khách')
    actor(s, 75, 370, 'Người chơi')
    s.line(75, 360, 75, 228, marker='tri')
    UC = {
        'reg': (340, 100, 105, 32, ['Đăng ký tài khoản']),
        'login': (340, 215, 105, 32, ['Đăng nhập']),
        'logout': (340, 380, 105, 32, ['Đăng xuất']),
        'home': (340, 475, 105, 32, ['Xem trang chủ']),
        'conn': (640, 100, 120, 38, ['Kết nối máy chủ', '(TLS, ghim chứng chỉ)']),
        'pw': (640, 265, 115, 38, ['Kiểm tra mật khẩu', '(PBKDF2)']),
        'dup': (640, 380, 120, 38, ['Từ chối đăng nhập', 'trùng tài khoản']),
        'hb': (895, 175, 112, 38, ['Duy trì heartbeat', '(PING/PONG)']),
        'rc': (895, 330, 112, 32, ['Tự kết nối lại']),
    }
    for k, (cx, cy, rx, ry, lines) in UC.items():
        ellipse(s, cx, cy, rx, ry, lines)
    g = lambda k: UC[k][:4]
    # actor associations
    for k in ('reg', 'login'):
        x, y = edge_point(*g(k), 100, 150)
        s.line(100, 150, x, y)
    for k in ('logout', 'home'):
        x, y = edge_point(*g(k), 100, 400)
        s.line(100, 400, x, y)
    dep(s, g('reg'), g('conn'), '«include»')
    dep(s, g('login'), g('conn'), '«include»')
    dep(s, g('login'), g('pw'), '«include»')
    dep(s, g('dup'), g('login'), '«extend»')
    dep(s, g('conn'), g('hb'), '«include»')
    dep(s, g('rc'), g('conn'), '«extend»')
    s.save('usecase_chitiet.svg')


# ---------------------------------------------------------------- class diagrams
def klass(svg, x, y, w, name, attrs, methods, stereo=None, fill='#EAF1FB'):
    lh = 19
    hh = 30 + (18 if stereo else 0)
    h = hh + max(1, len(attrs)) * lh + 10 + max(1, len(methods)) * lh + 10
    svg.rect(x, y, w, h, fill='#fff')
    svg.rect(x, y, w, hh, fill=fill)
    ty = y + 21
    if stereo:
        svg.text(x + w / 2, ty, stereo, 14, style='italic'); ty += 18
    svg.text(x + w / 2, ty, name, 16, weight='bold')
    yy = y + hh
    svg.line(x, yy, x + w, yy)
    for i, a in enumerate(attrs):
        svg.text(x + 8, yy + 18 + i * lh, a, 14.5, anchor='start')
    yy += max(1, len(attrs)) * lh + 10
    svg.line(x, yy, x + w, yy)
    for i, m in enumerate(methods):
        svg.text(x + 8, yy + 18 + i * lh, m, 14.5, anchor='start')
    return (x, y, w, h)


def class_client():
    s = SVG(1060, 740)
    s.text(530, 26, 'Phía Client (DartGame_Client) và thư viện dùng chung (DartGame_Common)', 17, weight='bold')
    main = klass(s, 20, 50, 250, 'Main', ['+socketHandler: SocketHandler', '-currentPage: String'],
                 ['+start(stage: Stage)', '+setRoot(fxml: String)', '+notifications(): Notifications'], '«Application»')
    login = klass(s, 20, 285, 250, 'LoginController', ['-usernameField, passwordField', '-connectionStatusLabel: Label'],
                  ['+initialize()', '+handleLogin()', '+handleRetry()', '+goToRegister()'])
    reg = klass(s, 20, 500, 250, 'RegisterController', ['-usernameField, passwordField'],
                ['+initialize()', '+handleRegister()', '+goToLogin()'])
    sh = klass(s, 330, 50, 370, 'SocketHandler',
               ['+loginUser: String', '+loggedInUserId: long', '-current: Connection',
                '-heartbeat: ScheduledExecutorService', '-connector: ExecutorService', '-authPending: boolean'],
               ['+ensureConnected(after: Runnable)', '+login(username, password)', '+register(username, password)',
                '+logout()', '+sendData(m: Message)', '-listen(c: Connection)', '-receive(m: Message)',
                '-failed(c: Connection)', '+shutdown()'])
    home = klass(s, 330, 545, 250, 'HomeController', ['-usernameLabel: Label'],
                 ['+initialize()', '+handleLogout()', '+handleStartGame()'])
    cs = klass(s, 760, 50, 285, 'ConnectionSettings', ['-PROFILE: Properties'],
               ['+host(), port(), transport()', '+websocketUri(): URI', '+tlsContext(): SSLContext',
                '+lastUsername(): String', '+rememberUsername(name)'])
    ts = klass(s, 760, 255, 285, 'TransportSecurity', [' '],
               ['+serverContext(store, pwd): SSLContext', '+pinnedContext(cert): SSLContext',
                '+connect(host, port, timeout, ctx): Socket', '+secure(socket, host, ...): Socket'], fill='#EEF6E8')
    ws = klass(s, 760, 455, 285, 'WebSocketSocket', ['-incoming: BlockingQueue<byte[]>'],
               ['+open(uri: URI, timeout): WebSocketSocket', '+getInputStream(), getOutputStream()'], '«extends Socket»', fill='#EEF6E8')
    msg = klass(s, 620, 590, 160, 'Message', ['-type: MessageType', '-requestId: String', '-payload: Object'], ['+of(type, payload)'], fill='#EEF6E8')
    lr = klass(s, 800, 600, 245, 'LoginRequest / AuthResponse', ['username, password /', 'success, userId, username, message'], [' '], fill='#EEF6E8')
    # relations
    s.line(270, 120, 330, 120, marker='open'); s.text(300, 112, '1', 14)
    s.line(145, 285, 145, main[1] + main[3], marker='open')          # Login -> Main
    s.line(270, 330, 330, 330, marker='open')                # Login -> SocketHandler
    s.line(270, 560, 300, 560); s.line(300, 560, 300, 380); s.line(300, 380, 330, 380, marker='open')  # Register -> SH
    s.line(455, 545, 455, 50 + sh[3] + 0, marker='open')       # Home -> SH
    s.line(700, 110, 760, 110, marker='open')               # SH -> ConnectionSettings
    s.line(700, 300, 760, 300, marker='open')               # SH -> TransportSecurity
    s.line(700, 360, 760, 470, marker='open')               # SH -> WebSocketSocket
    s.line(650, 50 + sh[3], 680, 590, marker='open')        # SH -> Message
    s.line(902, 50 + cs[3], 902, 255, dash=True, marker='open')  # CS uses TS
    s.text(530, 732, 'Màu xanh lá: lớp thuộc DartGame_Common, dùng chung cho Client và Server', 14, style='italic')
    s.save('class_client.svg')


def class_server():
    s = SVG(1060, 690)
    s.text(530, 26, 'Phía Server (DartGame_Server)', 17, weight='bold')
    main = klass(s, 20, 50, 270, 'DartGame_Server', [' '], ['+main(args: String[])'])
    bridge = klass(s, 20, 210, 270, 'WebSocketBridge', ['-backendPort: int'],
                   ['+onOpen(ws, handshake)', '+onMessage(ws, bytes)', '+close()'], '«extends WebSocketServer»')
    db = klass(s, 20, 440, 270, 'DBConnection', [' '],
               ['+getInstance(): DBConnection', '+getConnection(): Connection', '+initialize()'], '«singleton»')
    cl = klass(s, 350, 50, 340, 'Client', ['-socket: Socket', '-outgoing: BlockingQueue<Message>',
                                           '-loggedInUserId: long', '-loggedInUsername: String', '-closed: AtomicBoolean'],
               ['+run()', '-handleMessage(m: Message)', '+send(m: Message)', '+error(text: String)', '+close()',
                '+setLoggedInUserId(id: long)'], '«extends Thread»')
    uc = klass(s, 350, 440, 340, 'UserController', [' '], ['+handleMessage(c: Client, m: Message)'])
    ph = klass(s, 350, 555, 340, 'PasswordHasher', ['-ITERATIONS = 210000'],
               ['+hash(pwd): String', '+verify(pwd, encoded): boolean'])
    lobby = klass(s, 750, 50, 290, 'LobbyService', [' '],
                  ['+login(c: Client, id, name)', '+logout(c: Client, reason)'], '«singleton»')
    cm = klass(s, 750, 245, 290, 'ClientManager', ['-clients: ConcurrentHashMap', '<Long, Client>', '-statuses: Map<Long, PlayerStatus>'],
               ['+addClient(c): boolean', '+removeClient(c): boolean', '+getClient(id): Client', '+broadcastOnlineList()'], '«singleton»')
    usr = klass(s, 750, 500, 290, 'User', ['-id: long', '-username: String', '-passwordHash: String'], ['+get/set...'], '«model»')
    # relations
    s.line(290, 110, 350, 110, marker='open'); s.text(320, 102, '1..*', 14)        # Server creates Client
    s.line(155, 50 + main[3], 155, 210, dash=True, marker='open')                 # main starts bridge
    s.line(290, 300, 350, 300, dash=True, marker='open'); s.text(320, 292, 'TCP', 13)  # bridge -> Client via loopback
    s.line(520, 50 + cl[3], 520, 440, marker='open')                              # Client -> UserController
    s.line(520, 440 + uc[3], 520, 555, dash=True, marker='open')                  # UC -> PasswordHasher
    s.line(350, 480, 290, 480, dash=True, marker='open')                          # UC -> DB
    s.line(690, 470, 720, 470); s.line(720, 470, 720, 110); s.line(720, 110, 750, 110, dash=True, marker='open')  # UC -> Lobby
    s.line(690, 80, 750, 80, dash=True, marker='open')                            # Client.close -> Lobby.logout
    s.line(895, 50 + lobby[3], 895, 245, dash=True, marker='open')               # Lobby -> CM
    s.line(690, 300, 750, 300, marker='diamond'); s.text(705, 292, '0..*', 14)   # CM aggregates Client
    s.text(895, 488, '(ánh xạ một dòng của bảng users)', 14, style='italic')
    s.save('class_server.svg')


# ---------------------------------------------------------------- sequence diagrams
class Seq:
    def __init__(self, w, lifelines, top=20, gap=None):
        self.svg = SVG(w, 100)
        self.names = lifelines
        n = len(lifelines)
        margin = 80
        step = (w - 2 * margin) / (n - 1)
        self.x = [margin + i * step for i in range(n)]
        self.y = top + 70
        self.top = top
        self.frames = []

    def X(self, i):
        return self.x[i]

    def msg(self, a, b, label, ret=False, dy=34, label2=None):
        self.y += dy
        s = self.svg
        if a == b:
            x = self.X(a)
            s.add('<path d="M%g,%g h42 v24 h-36" fill="none" stroke="#000" stroke-width="1.4" marker-end="url(#fill)"/>' % (x + 6, self.y - 8))
            s.btext(x + 54, self.y + 2, label, anchor='start')
            if label2:
                s.btext(x + 54, self.y + 21, label2, anchor='start')
            self.y += 22
            return
        x1, x2 = self.X(a), self.X(b)
        off = 6 if x2 > x1 else -6
        mx = (x1 + x2) / 2
        s.btext(mx, self.y - 7, label)
        s.line(x1 + off, self.y, x2 - off, self.y, dash=ret, marker='open' if ret else 'fill')

    def note(self, i, text, w=260, dy=30):
        self.y += dy
        x = self.X(i) - w / 2
        self.svg.rect(x, self.y - 18, w, 26, fill='#FFF8DC', stroke='#A08A3C', sw=1)
        self.svg.text(x + w / 2, self.y, text, 14, style='italic')
        self.y += 4

    def frame_start(self, kind, cond, a, b):
        self.y += 30
        self.frames.append([kind, cond, a, b, self.y, []])

    def frame_else(self, cond):
        self.y += 26
        self.frames[-1][5].append((self.y, cond))

    def frame_end(self):
        kind, cond, a, b, y0, elses = self.frames.pop()
        self.y += 18
        s = self.svg
        x0, x1 = self.X(a) - 70, self.X(b) + 70
        s.rect(x0, y0 - 18, x1 - x0, self.y - y0 + 18, fill='none', stroke='#555', sw=1.2)
        s.add('<path d="M%g,%g h%g v14 l-8,8 h-%g z" fill="#E8E8E8" stroke="#555" stroke-width="1.2"/>' % (x0, y0 - 18, 52, 44))
        s.text(x0 + 24, y0 - 1, kind, 14, weight='bold')
        s.btext(x0 + 60, y0 - 1, '[' + cond + ']', 15, anchor='start', style='italic')
        for (yy, c) in elses:
            s.line(x0, yy - 14, x1, yy - 14, dash=True, color='#555', w=1.1)
            s.btext(x0 + 8, yy + 3, '[' + c + ']', 15, anchor='start', style='italic')

    def finish(self, name):
        s = self.svg
        bottom = self.y + 30
        heads = []
        for i, nm in enumerate(self.names):
            x = self.X(i)
            lines = nm.split('\n')
            w = max(len(l) for l in lines) * 8.0 + 18
            h = 22 + 18 * len(lines)
            heads.append('<rect x="%g" y="%g" width="%g" height="%g" fill="#EAF1FB" stroke="#1F3B63" stroke-width="1.5"/>' % (x - w / 2, self.top, w, h))
            for j, l in enumerate(lines):
                heads.append('<text x="%g" y="%g" font-family="%s" font-size="15.5" text-anchor="middle" font-weight="bold">%s</text>' % (x, self.top + 24 + j * 18, FONT, esc(l)))
            heads.append('<line x1="%g" y1="%g" x2="%g" y2="%g" stroke="#777" stroke-width="1.2" stroke-dasharray="5,5"/>' % (x, self.top + h, x, bottom))
        s.items = heads + s.items
        s.h = bottom + 15
        s.save(name)


def seq_login():
    q = Seq(1040, ['Người chơi', 'LoginController', 'SocketHandler', 'Client\n(luồng server)', 'UserController',
                   'LobbyService /\nClientManager', 'MySQL'])
    q.y += 10
    q.msg(1, 2, 'ensureConnected()')
    q.msg(2, 3, 'kết nối TCP (hoặc WSS) + bắt tay TLS')
    q.msg(3, 2, 'chứng chỉ máy chủ', ret=True)
    q.msg(2, 2, 'so khớp với chứng chỉ ghim', label2='(pinnedContext)')
    q.msg(2, 1, '"Đã kết nối. Mời bạn đăng nhập."', ret=True)
    q.msg(0, 1, 'nhập tên, mật khẩu')
    q.msg(1, 2, 'login(username, password)')
    q.msg(2, 3, 'LOGIN_REQUEST {LoginRequest}')
    q.msg(3, 4, 'handleMessage(client, m)')
    q.msg(4, 4, 'kiểm tra định dạng tên, mật khẩu')
    q.msg(4, 6, 'SELECT id, username, password_hash ... WHERE username=?')
    q.msg(6, 4, 'bản ghi (hoặc rỗng)', ret=True)
    q.msg(4, 4, 'PasswordHasher.verify(password, hash)')
    q.frame_start('alt', 'không có tài khoản hoặc sai mật khẩu', 2, 6)
    q.msg(4, 2, 'LOGIN_RESULT {success=false}', ret=True)
    q.frame_else('mật khẩu đúng')
    q.msg(4, 5, 'login(client, id, username)')
    q.msg(5, 5, 'addClient(client)', label2='(putIfAbsent theo userId)')
    q.frame_start('alt', 'userId đang có phiên ở máy khác', 2, 5)
    q.msg(5, 2, 'ERROR "Tài khoản đang đăng nhập ở máy khác."', ret=True)
    q.frame_else('thêm thành công')
    q.msg(5, 2, 'LOGIN_RESULT {success=true, userId, username}', ret=True)
    q.msg(5, 2, 'ONLINE_LIST (phát cho mọi client)', ret=True)
    q.msg(2, 1, 'Main.setRoot("home")', ret=True)
    q.frame_end()
    q.frame_end()
    q.finish('seq_dangnhap.svg')


def seq_heartbeat():
    q = Seq(960, ['Giao diện\n(Main)', 'SocketHandler', 'Client\n(luồng server)', 'LobbyService', 'ClientManager /\nGameService'])
    q.y += 10
    q.frame_start('loop', 'mỗi 5 giây, khi kết nối còn mở', 0, 4)
    q.msg(1, 2, 'PING')
    q.msg(2, 1, 'PONG (không chuyển lên giao diện)', ret=True)
    q.frame_end()
    q.note(2, 'Rớt mạng: không còn dữ liệu đi qua socket', w=360, dy=40)
    q.frame_start('par', 'server: không nhận được gì sau 45 giây', 0, 4)
    q.msg(2, 2, 'readObject() ném SocketTimeoutException', label2='→ close()')
    q.msg(2, 3, 'logout(client, "DISCONNECT")')
    q.msg(3, 4, 'removeClient(client)')
    q.msg(3, 4, 'leaveSession(): xử thua nếu đang đấu')
    q.msg(3, 4, 'broadcastOnlineList()')
    q.frame_else('client: lỗi đọc/ghi hoặc 35 giây không có dữ liệu')
    q.msg(1, 1, 'failed(connection): đóng socket,', label2='clearSession()')
    q.msg(1, 0, 'setRoot("login") + "Đã mất kết nối..."', ret=True)
    q.frame_end()
    q.frame_start('loop', 'mỗi 5 giây cho tới khi kết nối được', 0, 4)
    q.msg(1, 1, 'ensureConnected()')
    q.msg(1, 2, 'kết nối TCP/WSS + bắt tay TLS mới')
    q.frame_end()
    q.msg(1, 0, '"Đã kết nối. Mời bạn đăng nhập."', ret=True)
    q.finish('seq_matketnoi.svg')


def seq_register():
    q = Seq(900, ['Khách', 'RegisterController', 'SocketHandler', 'UserController', 'MySQL'])
    q.y += 10
    q.msg(0, 1, 'nhập tên, mật khẩu')
    q.msg(1, 2, 'register(username, password)')
    q.msg(2, 3, 'REGISTER_REQUEST {LoginRequest}')
    q.msg(3, 3, 'kiểm tra định dạng tên, mật khẩu')
    q.msg(3, 3, 'PasswordHasher.hash(password)', label2='(salt 16 byte, PBKDF2, 210000 vòng)')
    q.msg(3, 4, 'INSERT INTO users(username, password_hash)')
    q.frame_start('alt', 'thêm thành công', 1, 4)
    q.msg(3, 2, 'REGISTER_RESULT {success=true}', ret=True)
    q.msg(2, 1, 'setRoot("login"), thông báo thành công', ret=True)
    q.frame_else('trùng username (vi phạm UNIQUE)')
    q.msg(3, 2, 'REGISTER_RESULT {success=false, "Tên đăng nhập đã tồn tại."}', ret=True)
    q.frame_end()
    q.finish('seq_dangky.svg')


# ---------------------------------------------------------------- ER
def table(svg, x, y, w, name, cols, fill='#EAF1FB'):
    lh = 21
    svg.rect(x, y, w, 32 + len(cols) * lh + 8, fill='#fff')
    svg.rect(x, y, w, 32, fill=fill)
    svg.text(x + w / 2, y + 22, name, 17, weight='bold')
    for i, (key, c, t) in enumerate(cols):
        yy = y + 32 + 20 + i * lh
        if key:
            svg.text(x + 8, yy, key, 13, anchor='start', weight='bold', fill='#8B1A1A')
        svg.text(x + 46, yy, c, 15, anchor='start', weight='bold' if key == 'PK' else 'normal')
        svg.text(x + w - 8, yy, t, 14, anchor='end', fill='#333')
    return y + 32 + len(cols) * lh + 8


def er():
    s = SVG(1060, 560)
    users = [('PK', 'id', 'BIGINT AI'), ('UQ', 'username', 'VARCHAR(50)'), ('', 'password_hash', 'VARCHAR(255)'), ('', 'created_at', 'TIMESTAMP')]
    matches = [('PK', 'id', 'VARCHAR(255)'), ('', 'mode', 'VARCHAR(20)'), ('FK', 'player1_id', 'BIGINT'), ('FK', 'player2_id', 'BIGINT NULL'),
               ('', 'player2_type', 'VARCHAR(20)'), ('', 'winner_id', 'BIGINT NULL'), ('', 'winner_type', 'VARCHAR(20)'),
               ('', 'end_reason', 'VARCHAR(50)'), ('', 'started_at / ended_at', 'TIMESTAMP'), ('', 'session_id, round_index', ''),
               ('', 'bot_difficulty', 'VARCHAR(20)'), ('', 'player1/2_final_score', 'INT')]
    throws = [('PK', 'id', 'BIGINT AI'), ('FK', 'match_id', 'VARCHAR(255)'), ('FK', 'player_id', 'BIGINT NULL'), ('', 'actor_type', 'VARCHAR(20)'),
              ('', 'turn_number, dart_number', ''), ('', 'x, y', 'DOUBLE'), ('', 'rotation_degree', 'INT'), ('', 'hit_area', 'VARCHAR(50)'),
              ('', 'dart_score', 'INT'), ('', 'remaining_score_after', 'INT'), ('', 'is_bust', 'BOOLEAN'), ('', 'created_at', 'TIMESTAMP')]
    s.h = 700
    table(s, 40, 230, 290, 'users', users, fill='#F9E3C8')
    table(s, 740, 20, 300, 'matches', matches)
    table(s, 740, 390, 300, 'match_throws', throws)
    s.line(330, 262, 740, 109); s.line(330, 284, 740, 130); s.line(330, 306, 740, 479)
    s.line(890, 312, 890, 390)
    s.btext(510, 168, 'player1_id', 14.5, style='italic')
    s.btext(620, 248, 'player2_id (NULL nếu là Bot)', 14.5, style='italic')
    s.btext(590, 425, 'player_id (NULL nếu phi tiêu của Bot)', 14.5, style='italic')
    s.btext(900, 352, 'match_id', 14.5, anchor='start', style='italic')
    for x, y, t, an in [(342, 252, '1', 'start'), (350, 300, '0..1', 'start'), (352, 345, '0..1', 'start'),
                        (728, 98, '0..*', 'end'), (728, 152, '0..*', 'end'), (728, 462, '0..*', 'end'),
                        (898, 330, '1', 'start'), (898, 382, '0..*', 'start')]:
        s.text(x, y, t, 15, anchor=an)
    notes = ['PK: khóa chính; UQ: duy nhất; FK: khóa ngoại; AI: tự tăng.',
             'match_throws có UNIQUE(match_id, turn_number, dart_number)',
             'để một phi tiêu không bị ghi hai lần.',
             'matches.winner_id lưu id người thắng (không khai báo FK).']
    for i, t in enumerate(notes):
        s.text(40, 560 + i * 22, t, 14.5, anchor='start', style='italic')
    s.save('er.svg')


if __name__ == '__main__':
    usecase(); class_client(); class_server(); seq_login(); seq_heartbeat(); seq_register(); er()
