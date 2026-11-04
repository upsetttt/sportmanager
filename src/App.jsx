import { useEffect, useMemo, useState } from "react";
import {
  ArrowDown,
  ArrowRight,
  ArrowUpRight,
  CalendarDays,
  Check,
  ChevronDown,
  Clock3,
  Heart,
  MapPin,
  Menu,
  LogOut,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  Star,
  Users,
  UserRound,
  X,
} from "lucide-react";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";
const apiFetch = (path, options = {}) => {
  const headers = new Headers(options.headers || {});
  const token = sessionStorage.getItem("san-nha-minh-token");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  return fetch(`${API_URL}${path}`, { ...options, headers });
};
const sports = ["Tất cả", "Bóng đá", "Cầu lông", "Tennis", "Bóng rổ", "Bóng chuyền", "Pickleball"];
const fallbackVenues = [
  {
    id: 1,
    name: "Sân bóng The Pitch",
    sport: "Bóng đá",
    address: "Cầu Giấy, Hà Nội",
    pricePerHour: 350000,
    rating: 4.9,
    reviewCount: 128,
    imageUrl: "https://images.unsplash.com/photo-1522778119026-d647f0596c20?auto=format&fit=crop&w=1000&q=85",
    amenities: ["5 người", "Cỏ nhân tạo", "Có đèn"],
    featured: true,
  },
  {
    id: 2,
    name: "Smash Badminton Club",
    sport: "Cầu lông",
    address: "Đống Đa, Hà Nội",
    pricePerHour: 120000,
    rating: 4.8,
    reviewCount: 86,
    imageUrl: "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=1000&q=85",
    amenities: ["Sàn gỗ", "Điều hòa", "Cho thuê vợt"],
    featured: false,
  },
  {
    id: 3,
    name: "Riverside Tennis Club",
    sport: "Tennis",
    address: "Tây Hồ, Hà Nội",
    pricePerHour: 280000,
    rating: 4.9,
    reviewCount: 64,
    imageUrl: "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?auto=format&fit=crop&w=1000&q=85",
    amenities: ["Sân ngoài trời", "Phòng tắm", "Bãi đỗ xe"],
    featured: false,
  },
  {
    id: 4,
    name: "Green Court Basketball",
    sport: "Bóng rổ",
    address: "Ba Đình, Hà Nội",
    pricePerHour: 200000,
    rating: 4.7,
    reviewCount: 52,
    imageUrl: "https://images.unsplash.com/photo-1504450758481-7338eba7524a?auto=format&fit=crop&w=1000&q=85",
    amenities: ["Sân ngoài trời", "Bóng miễn phí", "Có đèn"],
    featured: false,
  },
];

const times = ["06:00", "07:00", "08:00", "09:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00"];
const today = new Date();
const localDate = (date) => {
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60_000).toISOString().slice(0, 10);
};
const formatPrice = (price) => new Intl.NumberFormat("vi-VN").format(price);

function App() {
  const [venues, setVenues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [selectedSport, setSelectedSport] = useState("Tất cả");
  const [searchText, setSearchText] = useState("");
  const [bookingVenue, setBookingVenue] = useState(null);
  const [showBookings, setShowBookings] = useState(false);
  const [notice, setNotice] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const [authUser, setAuthUser] = useState(() => {
    try {
      return JSON.parse(sessionStorage.getItem("san-nha-minh-account") || "null");
    } catch {
      return null;
    }
  });
  const [showAuth, setShowAuth] = useState(false);
  const [authIntent, setAuthIntent] = useState("login");
  const [pendingVenue, setPendingVenue] = useState(null);
  const [showOwnerDashboard, setShowOwnerDashboard] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [sortMode, setSortMode] = useState("recommended");
  const [showSortMenu, setShowSortMenu] = useState(false);
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [favorites, setFavorites] = useState(() => {
    try {
      const saved = JSON.parse(localStorage.getItem("san-nha-minh-favorites") || "[]");
      return Array.isArray(saved) ? saved : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    try {
      localStorage.setItem("san-nha-minh-favorites", JSON.stringify(favorites));
    } catch {
      setNotice("Không thể lưu sân yêu thích trong trình duyệt này.");
    }
  }, [favorites]);

  useEffect(() => {
    const loadVenues = async () => {
      try {
        const response = await apiFetch("/venues");
        if (!response.ok) throw new Error("Không thể tải danh sách sân.");
        const data = await response.json();
        setVenues(data);
      } catch {
        setLoadError("Chưa kết nối được máy chủ. Danh sách sân mẫu đang được hiển thị.");
        setVenues(fallbackVenues);
      } finally {
        setLoading(false);
      }
    };
    loadVenues();
  }, []);

  const filteredVenues = useMemo(() => {
    const matches = venues.filter((venue) => {
      const matchesSport = selectedSport === "Tất cả" || venue.sport === selectedSport;
      const text = `${venue.name} ${venue.address} ${venue.sport}`.toLocaleLowerCase("vi");
      return matchesSport && text.includes(searchText.trim().toLocaleLowerCase("vi"))
        && (!favoritesOnly || favorites.includes(venue.id));
    });
    if (sortMode === "price") return matches.sort((first, second) => first.pricePerHour - second.pricePerHour);
    if (sortMode === "rating") return matches.sort((first, second) => second.rating - first.rating);
    return matches.sort((first, second) => Number(second.featured) - Number(first.featured));
  }, [venues, selectedSport, searchText, favoritesOnly, favorites, sortMode]);

  const closeModals = () => {
    setBookingVenue(null);
    setShowBookings(false);
    setShowOwnerDashboard(false);
    setShowProfile(false);
  };

  const openBookings = () => {
    setMenuOpen(false);
    if (!authUser) {
      setAuthIntent("bookings");
      setShowAuth(true);
      return;
    }
    setShowBookings(true);
  };

  const bookVenue = (venue) => {
    if (!authUser) {
      setPendingVenue(venue);
      setAuthIntent("booking");
      setShowAuth(true);
      return;
    }
    setBookingVenue(venue);
  };

  const openOwnerDashboard = () => {
    if (!authUser) {
      setAuthIntent("owner");
      setShowAuth(true);
    } else if (authUser.role === "OWNER") {
      setShowOwnerDashboard(true);
    } else {
      setNotice("Chức năng quản lý sân dành cho tài khoản chủ sân.");
    }
  };

  const finishAuthentication = (result) => {
    sessionStorage.setItem("san-nha-minh-token", result.token);
    sessionStorage.setItem("san-nha-minh-account", JSON.stringify(result.account));
    setAuthUser(result.account);
    setShowAuth(false);
    if (authIntent === "bookings") setShowBookings(true);
    if (authIntent === "owner" && result.account.role === "OWNER") setShowOwnerDashboard(true);
    if (authIntent === "owner" && result.account.role !== "OWNER") setNotice("Tài khoản này chưa có quyền chủ sân. Thông tin tài khoản mẫu nằm trong biến OWNER_EMAIL và OWNER_PASSWORD của file .env.");
    if (pendingVenue) {
      setBookingVenue(pendingVenue);
      setPendingVenue(null);
    }
  };

  const logout = () => {
    sessionStorage.removeItem("san-nha-minh-token");
    sessionStorage.removeItem("san-nha-minh-account");
    setAuthUser(null);
    setNotice("Bạn đã đăng xuất.");
  };

  const updateAccount = (account) => {
    sessionStorage.setItem("san-nha-minh-account", JSON.stringify(account));
    setAuthUser(account);
    setShowProfile(false);
    setNotice("Thông tin tài khoản đã được cập nhật.");
  };

  const toggleFavorite = (venueId) => {
    setFavorites((current) => current.includes(venueId)
      ? current.filter((id) => id !== venueId)
      : [...current, venueId]);
  };

  return (
    <div className="app-shell container-fluid">
      <header className="topbar">
        <a className="brand" href="#" aria-label="Sân Nhà Mình - Trang chủ">
          <span className="brand-mark"><span /></span>
          <span>Sân nhà <span className="brand-light">mình</span></span>
        </a>
        <nav className={menuOpen ? "navigation open" : "navigation"}>
          <a className="nav-link active" href="#san-cho-ban" onClick={() => setMenuOpen(false)}>Tìm sân</a>
          <a className="nav-link" href="#cach-hoat-dong" onClick={() => setMenuOpen(false)}>Cách hoạt động</a>
          <button className="nav-link nav-button" onClick={openBookings}>Lịch đặt của tôi</button>
        </nav>
        <div className="nav-actions">
          <button className="host-link" onClick={openOwnerDashboard}>{authUser?.role === "OWNER" ? "Quản lý sân" : "Dành cho chủ sân"} <ArrowUpRight size={15} /></button>
          {authUser ? <><button className="account-button" onClick={() => setShowProfile(true)} title="Hồ sơ tài khoản"><span className="account-avatar"><UserRound size={15} /></span><span>{authUser.fullName.split(" ")[0]}</span></button><button className="logout-button" aria-label="Đăng xuất" title="Đăng xuất" onClick={logout}><LogOut size={15} /></button></> : <button className="profile-button" aria-label="Đăng nhập hoặc đăng ký" onClick={() => { setAuthIntent("login"); setShowAuth(true); }}><UserRound size={18} /></button>}
          <button className="profile-button menu-button" aria-label="Mở menu" onClick={() => setMenuOpen(!menuOpen)}><Menu size={19} /></button>
        </div>
      </header>

      <main>
        <section className="hero">
          <div className="hero-copy">
            <div className="eyebrow"><span className="eyebrow-dot" /> CHƠI THỂ THAO, SỐNG TRỌN ĐAM MÊ</div>
            <h1>Sân hay ở gần.<br /><span>Lịch chơi trong tay.</span></h1>
            <p>Tìm sân chất lượng, chọn khung giờ phù hợp và rủ hội bạn nhập cuộc. Mọi thứ gọn trong một chỗ.</p>
            <div className="hero-actions">
              <a className="button button-dark" href="#san-cho-ban">Khám phá sân <ArrowRight size={17} /></a>
              <div className="social-proof">
                <div className="avatar-stack"><span>H</span><span>M</span><span>A</span><span>+</span></div>
                <div><strong>2.4k+</strong><small>người chơi mỗi tuần</small></div>
              </div>
            </div>
            <div className="hero-benefits">
              <span><Check size={15} /> Đặt sân trong 1 phút</span>
              <span><Check size={15} /> Giá minh bạch</span>
            </div>
          </div>
          <div className="hero-visual">
            <img className="hero-image" src="https://images.unsplash.com/photo-1522778119026-d647f0596c20?auto=format&fit=crop&w=1400&q=90" alt="Sân bóng xanh trong ánh nắng" />
            <div className="image-shade" />
            <div className="floating-note"><span className="note-icon"><Sparkles size={17} /></span><span><strong>Sẵn sàng ra sân?</strong><small>Sân tốt, giờ đẹp đang chờ bạn</small></span></div>
            <div className="image-caption"><span className="caption-line" /> TÌM TRẬN ĐẤU TIẾP THEO CỦA BẠN</div>
          </div>
          <div className="hero-deco">PLAY<br />MORE.</div>
        </section>

        <section className="search-panel" aria-label="Tìm kiếm sân thể thao">
          <label className="search-field">
            <MapPin size={19} />
            <span className="field-content"><small>KHU VỰC</small><input className="form-control" aria-label="Khu vực hoặc tên sân" placeholder="Tên sân, quận, thành phố..." value={searchText} onChange={(event) => setSearchText(event.target.value)} /></span>
          </label>
          <label className="search-field sport-select">
            <span className="sport-icon">◎</span>
            <span className="field-content"><small>MÔN THỂ THAO</small><select className="form-select" aria-label="Chọn môn thể thao" value={selectedSport} onChange={(event) => setSelectedSport(event.target.value)}>{sports.map((sport) => <option key={sport}>{sport}</option>)}</select></span>
            <ChevronDown size={16} className="select-chevron" />
          </label>
          <a className="button button-green search-button" href="#san-cho-ban"><Search size={17} /> Tìm sân ngay</a>
        </section>

        <section className="venues-section" id="san-cho-ban">
          <div className="section-heading">
            <div><div className="eyebrow section-eyebrow">CHỌN SÂN, LÊN KÈO</div><h2>Sân chơi dành cho bạn<span>.</span></h2><p>Những địa điểm được người chơi yêu thích tuần này.</p></div>
            <a className="text-link" href="#danh-sach">Xem tất cả sân <ArrowRight size={16} /></a>
          </div>
          <div className="sports-filter" role="group" aria-label="Lọc theo môn thể thao">
            {sports.map((sport) => <button key={sport} className={sport === selectedSport ? "filter-chip selected" : "filter-chip"} onClick={() => setSelectedSport(sport)}>{sport}</button>)}
            <button className={favoritesOnly ? "filter-chip favorite-filter selected" : "filter-chip favorite-filter"} onClick={() => setFavoritesOnly(!favoritesOnly)}><Heart size={13} /> Đã lưu{favorites.length ? ` (${favorites.length})` : ""}</button>
            <span className="filter-spacer" /><div className="sort-picker"><button className="filter-control" aria-expanded={showSortMenu} onClick={() => setShowSortMenu(!showSortMenu)}><SlidersHorizontal size={15} /> Sắp xếp <ChevronDown size={13} /></button>{showSortMenu && <div className="sort-menu">{[["recommended", "Đề xuất"], ["price", "Giá thấp trước"], ["rating", "Đánh giá cao"]].map(([value, label]) => <button key={value} className={sortMode === value ? "sort-option selected" : "sort-option"} onClick={() => { setSortMode(value); setShowSortMenu(false); }}>{label}{sortMode === value && <Check size={13} />}</button>)}</div>}</div>
          </div>
          {loadError && <p className="inline-notice" role="status">{loadError}</p>}
          {loading ? <div className="loading-state">Đang tìm sân phù hợp...</div> : filteredVenues.length ? (
            <div className="venue-grid row row-cols-1 row-cols-sm-2 row-cols-xl-4 g-3" id="danh-sach">
              {filteredVenues.map((venue, index) => <div className="col" key={venue.id}><VenueCard venue={venue} index={index} isFavorite={favorites.includes(venue.id)} onToggleFavorite={() => toggleFavorite(venue.id)} onBook={() => bookVenue(venue)} /></div>)}
            </div>
          ) : <div className="empty-state"><Search size={23} /><strong>Chưa tìm thấy sân phù hợp</strong><span>Thử đổi môn thể thao hoặc từ khóa tìm kiếm nhé.</span></div>}
          <div className="center-action"><a className="button button-outline" href="#danh-sach">Khám phá thêm sân <ArrowDown size={16} /></a></div>
        </section>

        <section className="how-section" id="cach-hoat-dong">
          <div className="how-intro"><div className="eyebrow">ĐƠN GIẢN HÓA CUỘC CHƠI</div><h2>Từ “lên kèo”<br />đến “vào sân”.</h2><p>Không gọi điện lòng vòng, không chờ xác nhận lâu. Đặt sân nhanh để dành thời gian cho trận đấu.</p></div>
          <div className="steps">
            <div className="step"><span className="step-number">01</span><span className="step-icon"><Search size={19} /></span><h3>Tìm sân phù hợp</h3><p>Chọn môn thể thao và khu vực bạn muốn chơi.</p></div>
            <div className="step"><span className="step-number">02</span><span className="step-icon"><CalendarDays size={19} /></span><h3>Chọn giờ, đặt lịch</h3><p>Xem giá rõ ràng và giữ chỗ chỉ với vài thao tác.</p></div>
            <div className="step"><span className="step-number">03</span><span className="step-icon"><Users size={19} /></span><h3>Rủ hội, ra sân</h3><p>Lịch đã sẵn sàng — giờ là lúc tận hưởng cuộc chơi.</p></div>
          </div>
        </section>

        <section className="trust-strip">
          <div className="trust-icon"><ShieldCheck size={22} /></div><div><strong>Đặt sân an tâm, cuộc chơi trọn vẹn</strong><span>Thông tin rõ ràng · Giá công khai · Xác nhận tức thì</span></div><div className="trust-rating"><Star size={16} fill="currentColor" /><strong>4.9/5</strong><span>từ người chơi</span></div>
        </section>
      </main>
      <footer className="footer"><a className="brand footer-brand" href="#"><span className="brand-mark"><span /></span><span>Sân nhà <span className="brand-light">mình</span></span></a><span>© 2026 Sân Nhà Mình. Chơi vui, chơi khỏe.</span><a href="#cach-hoat-dong">Về chúng tôi <ArrowUpRight size={14} /></a></footer>

      {notice && <div className="toast" role="status">{notice}<button onClick={() => setNotice("")} aria-label="Đóng"><X size={15} /></button></div>}
      {showAuth && <AuthModal ownerLogin={authIntent === "owner"} onClose={() => { setShowAuth(false); setPendingVenue(null); }} onSuccess={finishAuthentication} />}
      {bookingVenue && <BookingModal venue={bookingVenue} account={authUser} onClose={closeModals} onSuccess={(message) => { setBookingVenue(null); setNotice(message); }} />}
      {showBookings && <BookingsModal onClose={closeModals} />}
      {showOwnerDashboard && <OwnerDashboard onClose={closeModals} onChanged={() => { setLoading(true); apiFetch("/venues").then((response) => response.json()).then(setVenues).catch(() => setNotice("Không thể cập nhật danh sách sân." )).finally(() => setLoading(false)); }} />}
      {showProfile && <ProfileModal account={authUser} onClose={closeModals} onSaved={updateAccount} onBookings={() => { setShowProfile(false); setShowBookings(true); }} />}
    </div>
  );
}

function VenueCard({ venue, index, isFavorite, onToggleFavorite, onBook }) {
  return (
    <article className={`venue-card card-${index % 4}`}>
      <div className="venue-image-wrap">
        <img src={venue.imageUrl || fallbackVenues[0].imageUrl} alt={venue.name} loading="lazy" />
        <span className="sport-badge">{venue.sport}</span>
        <button className={isFavorite ? "favorite-button favorited" : "favorite-button"} aria-label={isFavorite ? `Bỏ lưu ${venue.name}` : `Lưu ${venue.name}`} aria-pressed={isFavorite} onClick={onToggleFavorite}>{isFavorite ? "♥" : "♡"}</button>
        {venue.featured && <span className="featured-badge"><Sparkles size={12} /> NỔI BẬT</span>}
      </div>
      <div className="venue-info">
        <div className="venue-title-row"><h3>{venue.name}</h3><span className="rating"><Star size={13} fill="currentColor" /> {venue.rating}</span></div>
        <div className="venue-address"><MapPin size={14} /> {venue.address}</div>
        <div className="amenities">{(venue.amenities || []).slice(0, 3).map((amenity) => <span key={amenity}>{amenity}</span>)}</div>
        <div className="venue-bottom"><div><strong>{formatPrice(venue.pricePerHour)}đ</strong><span> / giờ</span><small>{venue.reviewCount || 0} đánh giá</small></div><button className="book-button" onClick={onBook}>Đặt sân <ArrowUpRight size={15} /></button></div>
      </div>
    </article>
  );
}

function AuthModal({ ownerLogin, onClose, onSuccess }) {
  const [mode, setMode] = useState("login");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const path = mode === "register" ? "/auth/register" : "/auth/login";
      const body = mode === "register" ? { fullName, email, phone, password } : { email, password };
      const response = await apiFetch(path, { method: "POST", body: JSON.stringify(body) });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(result.message || result.detail || "Thông tin đăng nhập không hợp lệ.");
      onSuccess(result);
    } catch (requestError) {
      setError(requestError.message || "Không thể kết nối máy chủ.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="modal auth-modal" role="dialog" aria-modal="true" aria-labelledby="auth-title">
        <button className="modal-close" aria-label="Đóng" onClick={onClose}><X size={19} /></button>
        <div className="modal-eyebrow">{mode === "register" ? "THAM GIA CỘNG ĐỒNG" : "CHÀO MỪNG BẠN TRỞ LẠI"}</div>
        <h2 id="auth-title">{mode === "register" ? "Tạo tài khoản" : "Đăng nhập"}</h2>
        <p className="modal-description">{mode === "register" ? "Tạo tài khoản khách hàng để đặt sân và quản lý lịch chơi." : ownerLogin ? "Đăng nhập bằng tài khoản quản lý đã được khởi tạo." : "Đăng nhập để quản lý lịch đặt và tiếp tục cuộc chơi."}</p>
        {ownerLogin && mode === "login" && <p className="owner-login-hint">Thông tin tài khoản mẫu nằm trong biến <code>OWNER_EMAIL</code> và <code>OWNER_PASSWORD</code> ở file .env.</p>}
        <form onSubmit={submit}>
          {mode === "register" && <>
            <label className="form-label">Họ và tên<input className="form-control" value={fullName} onChange={(event) => setFullName(event.target.value)} required maxLength={100} autoComplete="name" /></label>
            <label className="form-label">Số điện thoại<input className="form-control" type="tel" pattern="[0-9+() .-]{8,20}" title="Nhập số điện thoại từ 8 đến 20 ký tự." value={phone} onChange={(event) => setPhone(event.target.value)} required maxLength={20} autoComplete="tel" /></label>
          </>}
          <label className="form-label">Email<input className="form-control" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required maxLength={150} autoComplete="email" /></label>
          <label className="form-label">Mật khẩu<input className="form-control" type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={mode === "register" ? 8 : 1} maxLength={72} autoComplete={mode === "register" ? "new-password" : "current-password"} /></label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="button button-green submit-booking" type="submit" disabled={submitting}>{submitting ? "Đang xử lý..." : mode === "register" ? "Tạo tài khoản" : "Đăng nhập"} <ArrowRight size={16} /></button>
        </form>
        {!ownerLogin && <p className="auth-switch">{mode === "register" ? "Đã có tài khoản?" : "Chưa có tài khoản?"} <button onClick={() => { setError(""); setMode(mode === "register" ? "login" : "register"); }}> {mode === "register" ? "Đăng nhập" : "Đăng ký ngay"}</button></p>}
      </section>
    </div>
  );
}

function ProfileModal({ account, onClose, onSaved, onBookings }) {
  const [fullName, setFullName] = useState(account.fullName);
  const [phone, setPhone] = useState(account.phone);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    apiFetch("/auth/me")
      .then(async (response) => {
        const result = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(result.message || result.detail || "Không thể tải hồ sơ.");
        if (active) {
          setFullName(result.fullName);
          setPhone(result.phone);
          sessionStorage.setItem("san-nha-minh-account", JSON.stringify(result));
        }
      })
      .catch((requestError) => {
        if (active) setError(requestError.message || "Không thể kết nối máy chủ.");
      })
      .finally(() => { if (active) setLoading(false); });
    setLoading(true);
    return () => { active = false; };
  }, []);

  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const response = await apiFetch("/auth/me", {
        method: "PUT",
        body: JSON.stringify({ fullName, phone }),
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(result.message || result.detail || "Không thể cập nhật hồ sơ.");
      onSaved(result);
    } catch (requestError) {
      setError(requestError.message || "Không thể kết nối máy chủ.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="modal profile-modal" role="dialog" aria-modal="true" aria-labelledby="profile-title">
        <button className="modal-close" aria-label="Đóng" onClick={onClose}><X size={19} /></button>
        <div className="modal-eyebrow">THÔNG TIN CỦA BẠN</div>
        <h2 id="profile-title">Hồ sơ tài khoản</h2>
        <p className="modal-description">Cập nhật thông tin liên hệ dùng cho các lịch đặt sân.</p>
        <form onSubmit={save}>
          <label className="form-label">Họ và tên<input className="form-control" value={fullName} onChange={(event) => setFullName(event.target.value)} required maxLength={100} /></label>
          <label className="form-label">Email đăng nhập<input className="form-control" value={account.email} readOnly /></label>
          <label className="form-label">Số điện thoại<input className="form-control" type="tel" pattern="[0-9+() .-]{8,20}" title="Nhập số điện thoại từ 8 đến 20 ký tự." value={phone} onChange={(event) => setPhone(event.target.value)} required maxLength={20} /></label>
          <label className="form-label">Loại tài khoản<input className="form-control" value={account.role === "OWNER" ? "Chủ sân" : "Khách hàng"} readOnly /></label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <div className="profile-actions">
            <button type="button" className="button button-outline" onClick={onBookings}>Lịch đặt của tôi</button>
            <button type="submit" className="button button-green" disabled={saving || loading}>{saving ? "Đang lưu..." : "Lưu hồ sơ"} <Check size={15} /></button>
          </div>
        </form>
      </section>
    </div>
  );
}

function BookingModal({ venue, account, onClose, onSuccess }) {
  const [date, setDate] = useState(localDate(today));
  const [time, setTime] = useState("");
  const [duration, setDuration] = useState(1);
  const [bookedSlots, setBookedSlots] = useState([]);
  const [availabilityLoading, setAvailabilityLoading] = useState(true);
  const [availabilityError, setAvailabilityError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    const loadAvailability = async () => {
      setAvailabilityLoading(true);
      setAvailabilityError("");
      setBookedSlots([]);
      setTime("");
      try {
        const response = await apiFetch(`/venues/${venue.id}/availability?date=${encodeURIComponent(date)}`);
        const result = await response.json().catch(() => []);
        if (!response.ok) throw new Error(result.message || result.detail || "Không thể tải giờ trống của sân.");
        if (active) setBookedSlots(result);
      } catch (requestError) {
        if (active) setAvailabilityError(requestError.message || "Không thể kết nối máy chủ.");
      } finally {
        if (active) setAvailabilityLoading(false);
      }
    };
    loadAvailability();
    return () => { active = false; };
  }, [venue.id, date]);

  const isSlotUnavailable = (slot, hours = 1) => {
    if (new Date(`${date}T${slot}:00`) < new Date()) return true;
    const [startHour, startMinute] = slot.split(":").map(Number);
    const startMinutes = startHour * 60 + startMinute;
    const endMinutes = startMinutes + hours * 60;
    return bookedSlots.some((booked) => {
      const [busyStartHour, busyStartMinute] = booked.startTime.split(":").map(Number);
      const [busyEndHour, busyEndMinute] = booked.endTime.split(":").map(Number);
      const busyStart = busyStartHour * 60 + busyStartMinute;
      const busyEnd = busyEndHour * 60 + busyEndMinute;
      return startMinutes < busyEnd && endMinutes > busyStart;
    });
  };

  const submit = async (event) => {
    event.preventDefault();
    setError("");
    if (availabilityError || availabilityLoading || !time || isSlotUnavailable(time, duration)) {
      setError("Khung giờ vừa thay đổi hoặc chưa kiểm tra được. Vui lòng chọn lại giờ.");
      return;
    }
    setSubmitting(true);
    try {
      const response = await apiFetch("/bookings", {
        method: "POST",
        body: JSON.stringify({ venueId: venue.id, bookingDate: date, startTime: time, durationHours: duration }),
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(result.message || result.detail || "Đặt sân chưa thành công. Vui lòng thử lại.");
      onSuccess(`Đặt sân thành công! Mã lịch: ${result.id}.`);
    } catch (requestError) {
      setError(requestError.message || "Không thể kết nối máy chủ. Vui lòng thử lại.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="modal booking-modal" role="dialog" aria-modal="true" aria-labelledby="booking-title">
        <button className="modal-close" aria-label="Đóng" onClick={onClose}><X size={19} /></button>
        <div className="modal-eyebrow">GIỮ CHỖ NGAY</div><h2 id="booking-title">Đặt sân của bạn</h2>
        <div className="booking-venue"><img src={venue.imageUrl} alt="" /><span><strong>{venue.name}</strong><small><MapPin size={12} /> {venue.address}</small></span><b>{formatPrice(venue.pricePerHour)}đ<small>/giờ</small></b></div>
        <form onSubmit={submit}>
          <label className="form-label">Ngày chơi<input className="form-control" type="date" min={localDate(today)} value={date} onChange={(event) => { setDate(event.target.value); setDuration(1); }} required /></label>
          <div className="form-label">Chọn giờ bắt đầu<div className="availability-note">{availabilityLoading ? "Đang kiểm tra giờ trống..." : availabilityError || "Giờ đã có người đặt được làm mờ."}</div><div className="time-grid">{times.map((slot) => { const unavailable = availabilityLoading || Boolean(availabilityError) || isSlotUnavailable(slot); return <button type="button" key={slot} disabled={unavailable} title={unavailable ? "Khung giờ không còn trống" : "Chọn giờ này"} className={`${time === slot ? "time-slot chosen" : "time-slot"}${unavailable ? " unavailable" : ""}`} onClick={() => { setTime(slot); setDuration(1); }}>{slot}</button>; })}</div></div>
          <div className="form-row"><label className="form-label">Người đặt<input className="form-control" value={account.fullName} readOnly /></label><label className="form-label">Số điện thoại<input className="form-control" value={account.phone} readOnly /></label></div>
          <label className="form-label">Thời lượng<select className="form-select" value={duration} onChange={(event) => setDuration(Number(event.target.value))}>{Array.from({ length: Math.max(1, Math.min(4, 23 - Number(time.slice(0, 2)))) }, (_, index) => index + 1).filter((hours) => !time || !isSlotUnavailable(time, hours)).map((hours) => <option value={hours} key={hours}>{hours} giờ</option>)}</select></label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <div className="booking-total"><span><Clock3 size={15} /> Thời lượng {duration} giờ</span><strong>{formatPrice(venue.pricePerHour * duration)}đ</strong></div>
          <button className="button button-green submit-booking" type="submit" disabled={!time || submitting || availabilityLoading || Boolean(availabilityError) || isSlotUnavailable(time, duration)}>{submitting ? "Đang giữ chỗ..." : "Xác nhận đặt sân"} <ArrowRight size={16} /></button>
          {!time && <small className="choose-time-hint">Vui lòng chọn giờ chơi để tiếp tục</small>}
        </form>
      </section>
    </div>
  );
}

function BookingsModal({ onClose }) {
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [cancellingId, setCancellingId] = useState(null);

  const loadBookings = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await apiFetch("/bookings");
      const result = await response.json().catch(() => []);
      if (!response.ok) throw new Error("Không thể tải lịch đặt. Vui lòng thử lại.");
      setBookings(result);
    } catch (requestError) {
      setError(requestError.message || "Không thể kết nối máy chủ.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadBookings(); }, []);

  const cancelBooking = async (bookingId) => {
    if (!window.confirm("Bạn có chắc muốn hủy lịch đặt này?")) return;
    setError("");
    setCancellingId(bookingId);
    try {
      const response = await apiFetch(`/bookings/${bookingId}`, { method: "DELETE" });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(result.message || "Không thể hủy lịch đặt. Vui lòng thử lại.");
      setBookings((current) => current.map((booking) => booking.id === bookingId ? result : booking));
    } catch (requestError) {
      setError(requestError.message || "Không thể kết nối máy chủ.");
    } finally {
      setCancellingId(null);
    }
  };

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="modal bookings-modal" role="dialog" aria-modal="true" aria-labelledby="bookings-title">
        <button className="modal-close" aria-label="Đóng" onClick={onClose}><X size={19} /></button>
        <div className="modal-eyebrow">KẾ HOẠCH CỦA BẠN</div><h2 id="bookings-title">Lịch đặt của tôi</h2>
        <p className="modal-description">Lịch đặt được liên kết với tài khoản của bạn.</p>
        <button className="button button-outline refresh-bookings" onClick={loadBookings} disabled={loading}>{loading ? "Đang tải..." : "Làm mới lịch"}</button>
        {error && <p className="form-error" role="alert">{error}</p>}
        {bookings.length ? <div className="booking-list">{bookings.map((booking) => <article className="booking-list-item" key={booking.id}><div className="booking-date"><strong>{booking.bookingDate.slice(8, 10)}</strong><span>{booking.bookingDate.slice(5, 7)}</span></div><div><strong>{booking.venueName}</strong><span><CalendarDays size={13} /> {booking.bookingDate} · {booking.startTime.slice(0, 5)}–{booking.endTime.slice(0, 5)}</span><small>Mã đặt sân: #{booking.id} · {booking.status === "CANCELLED" ? "Đã hủy" : "Đã xác nhận"}</small></div><div className="booking-item-action"><b>{formatPrice(booking.totalPrice)}đ</b>{booking.status === "CONFIRMED" && <button onClick={() => cancelBooking(booking.id)} disabled={cancellingId === booking.id}>{cancellingId === booking.id ? "Đang hủy..." : "Hủy lịch"}</button>}</div></article>)}</div> : !loading && <div className="empty-bookings">Bạn chưa có lịch đặt nào. Hãy chọn một sân để bắt đầu nhé.</div>}
      </section>
    </div>
  );
}

function OwnerDashboard({ onClose, onChanged }) {
  const [venues, setVenues] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [form, setForm] = useState({ name: "", sport: "Bóng đá", address: "", pricePerHour: "", imageUrl: "", amenities: "" });
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadData = async () => {
    setLoading(true);
    setError("");
    try {
      const [venueResponse, bookingResponse] = await Promise.all([
        apiFetch("/venues/mine"),
        apiFetch("/bookings/managed"),
      ]);
      if (!venueResponse.ok || !bookingResponse.ok) throw new Error("Không thể tải dữ liệu quản lý.");
      const [venueData, bookingData] = await Promise.all([venueResponse.json(), bookingResponse.json()]);
      setVenues(venueData);
      setBookings(bookingData);
    } catch (requestError) {
      setError(requestError.message || "Không thể kết nối máy chủ.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  const updateForm = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  const resetForm = () => {
    setEditingId(null);
    setForm({ name: "", sport: "Bóng đá", address: "", pricePerHour: "", imageUrl: "", amenities: "" });
  };

  const saveVenue = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    setMessage("");
    const body = {
      ...form,
      pricePerHour: Number(form.pricePerHour),
      amenities: form.amenities.split(",").map((amenity) => amenity.trim()).filter(Boolean),
    };
    try {
      const response = await apiFetch(editingId ? `/venues/${editingId}` : "/venues", {
        method: editingId ? "PUT" : "POST",
        body: JSON.stringify(body),
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(result.message || result.detail || "Không thể lưu thông tin sân.");
      setMessage(editingId ? "Đã cập nhật sân." : "Đã thêm sân mới.");
      resetForm();
      await loadData();
      onChanged();
    } catch (requestError) {
      setError(requestError.message || "Không thể kết nối máy chủ.");
    } finally {
      setSaving(false);
    }
  };

  const editVenue = (venue) => {
    setEditingId(venue.id);
    setForm({
      name: venue.name,
      sport: venue.sport,
      address: venue.address,
      pricePerHour: String(venue.pricePerHour),
      imageUrl: venue.imageUrl || "",
      amenities: (venue.amenities || []).join(", "),
    });
    setError("");
  };

  const archiveVenue = async (venue) => {
    if (!window.confirm(`Ẩn sân "${venue.name}" khỏi danh sách đặt sân?`)) return;
    setError("");
    try {
      const response = await apiFetch(`/venues/${venue.id}`, { method: "DELETE" });
      if (!response.ok) throw new Error("Không thể ẩn sân này.");
      setMessage("Đã ẩn sân khỏi danh sách.");
      if (editingId === venue.id) resetForm();
      await loadData();
      onChanged();
    } catch (requestError) {
      setError(requestError.message || "Không thể kết nối máy chủ.");
    }
  };

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="modal owner-modal" role="dialog" aria-modal="true" aria-labelledby="owner-title">
        <button className="modal-close" aria-label="Đóng" onClick={onClose}><X size={19} /></button>
        <div className="modal-eyebrow">KHÔNG GIAN CHỦ SÂN</div>
        <h2 id="owner-title">Quản lý sân của bạn</h2>
        <p className="modal-description">Cập nhật thông tin sân và theo dõi các lịch đặt.</p>
        {error && <p className="form-error" role="alert">{error}</p>}
        {message && <p className="dashboard-success" role="status">{message}</p>}
        <div className="owner-layout">
          <div className="owner-list-column">
            <div className="dashboard-subheading"><strong>Sân đang quản lý</strong><span>{venues.length} sân</span></div>
            {loading ? <p className="dashboard-empty">Đang tải dữ liệu...</p> : venues.length ? <div className="owner-venue-list">{venues.map((venue) => <article className="owner-venue" key={venue.id}><img src={venue.imageUrl || fallbackVenues[0].imageUrl} alt="" /><div><strong>{venue.name}</strong><span>{venue.sport} · {formatPrice(venue.pricePerHour)}đ/giờ</span><small>{venue.address}</small></div><div className="owner-actions"><button onClick={() => editVenue(venue)}>Sửa</button><button onClick={() => archiveVenue(venue)}>Ẩn</button></div></article>)}</div> : <p className="dashboard-empty">Bạn chưa đăng sân nào. Thêm sân đầu tiên nhé.</p>}
            <div className="dashboard-subheading booking-subheading"><strong>Lịch đặt tại sân của bạn</strong><span>{bookings.length} lịch</span></div>
            {bookings.length ? <div className="owner-booking-list">{bookings.map((booking) => <article className="owner-booking" key={booking.id}><div><strong>{booking.venueName}</strong><span>{booking.bookingDate} · {booking.startTime.slice(0, 5)}–{booking.endTime.slice(0, 5)}</span><small>{booking.customerName} · {booking.email}</small></div><span className={booking.status === "CANCELLED" ? "booking-status cancelled" : "booking-status"}>{booking.status === "CANCELLED" ? "Đã hủy" : "Đã xác nhận"}</span></article>)}</div> : !loading && <p className="dashboard-empty">Chưa có lịch đặt nào.</p>}
          </div>
          <form className="owner-form" onSubmit={saveVenue}>
            <div className="dashboard-subheading"><strong>{editingId ? "Chỉnh sửa sân" : "Thêm sân mới"}</strong>{editingId && <button type="button" onClick={resetForm}>Hủy sửa</button>}</div>
            <label className="form-label">Tên sân<input className="form-control" name="name" value={form.name} onChange={updateForm} required maxLength={120} /></label>
            <label className="form-label">Môn thể thao<select className="form-select" name="sport" value={form.sport} onChange={updateForm}><option>Bóng đá</option><option>Cầu lông</option><option>Tennis</option><option>Bóng rổ</option><option>Bóng chuyền</option><option>Pickleball</option></select></label>
            <label className="form-label">Địa chỉ<input className="form-control" name="address" value={form.address} onChange={updateForm} required maxLength={200} /></label>
            <label className="form-label">Giá mỗi giờ (VNĐ)<input className="form-control" name="pricePerHour" type="number" min="1000" step="1000" value={form.pricePerHour} onChange={updateForm} required /></label>
            <label className="form-label">Ảnh sân (URL)<input className="form-control" name="imageUrl" type="url" value={form.imageUrl} onChange={updateForm} maxLength={500} placeholder="https://..." /></label>
            <label className="form-label">Tiện ích, cách nhau bằng dấu phẩy<input className="form-control" name="amenities" value={form.amenities} onChange={updateForm} placeholder="Có đèn, Bãi đỗ xe" /></label>
            <button className="button button-green submit-booking" type="submit" disabled={saving}>{saving ? "Đang lưu..." : editingId ? "Lưu thay đổi" : "Đăng sân"} <ArrowRight size={16} /></button>
          </form>
        </div>
      </section>
    </div>
  );
}

export default App;
