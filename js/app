(() => {
  const root = document.documentElement;
  const $ = (s, c = document) => c.querySelector(s);
  const $$ = (s, c = document) => [...c.querySelectorAll(s)];
  const store = {
    get: k => { try { return localStorage.getItem(k); } catch { return null; } },
    set: (k, v) => { try { localStorage.setItem(k, v); } catch { /* modo privado */ } }
  };
  root.classList.add("js");

  // Tema (classe inicial já aplicada no <head> para evitar flash)
  const themeToggle = $("#themeToggle");
  const paintTheme = () => {
    if (!themeToggle) return;
    const dark = root.classList.contains("dark");
    themeToggle.innerHTML = `<span aria-hidden="true">${dark ? "☀" : "◐"}</span> ${dark ? "Tela clara" : "Tela escura"}`;
    themeToggle.setAttribute("aria-label", dark ? "Ativar tela clara" : "Ativar tela escura");
  };
  paintTheme();
  themeToggle?.addEventListener("click", () => {
    root.classList.toggle("dark");
    store.set("latam-demo-theme", root.classList.contains("dark") ? "dark" : "light");
    paintTheme();
  });

  // Menu móvel: fecha com link, Escape e clique fora
  const menuToggle = $("#menuToggle"), mainNav = $("#mainNav");
  const setMenu = open => {
    mainNav?.classList.toggle("open", open);
    menuToggle?.setAttribute("aria-expanded", String(open));
  };
  menuToggle?.addEventListener("click", () => setMenu(!mainNav.classList.contains("open")));
  $$("a", mainNav || document.createElement("div")).forEach(a => a.addEventListener("click", () => setMenu(false)));
  document.addEventListener("click", e => {
    if (mainNav?.classList.contains("open") && !e.target.closest(".site-header")) setMenu(false);
  });

  // Filtro do catálogo com botões de alternância e contador anunciado
  const cards = $$(".product-card"), count = $("#resultCount");
  $$(".category-tab").forEach(tab => tab.addEventListener("click", () => {
    $$(".category-tab").forEach(t => t.setAttribute("aria-pressed", String(t === tab)));
    $$(".category-tab").forEach(t => t.classList.toggle("active", t === tab));
    const f = tab.dataset.filter;
    let n = 0;
    cards.forEach(c => { c.hidden = f !== "todos" && c.dataset.category !== f; if (!c.hidden) n++; });
    if (count) count.textContent = `${n} ${n === 1 ? "categoria exibida" : "categorias exibidas"}`;
  }));

  // Modal acessível: foco, trap de Tab, Escape, scroll travado e retorno de foco
  const modal = $("#productModal");
  const texts = {
    "Jaquetas": "Categoria visual para representar a seleção de roupas para destinos frios. O case não detalha modelos, materiais ou preços.",
    "Calças": "Categoria de inverno usada apenas para demonstrar a navegação do catálogo. As especificações comerciais não são definidas no material.",
    "Botas": "Categoria ilustrativa para a página de roupas. O objetivo é mostrar como a escolha poderia ser apresentada de forma simples.",
    "Toucas": "Categoria complementar de inverno para a demonstração da interface. Sem especificações ou valores não presentes no case."
  };
  let opener = null;
  const focusables = () => $$("a[href],button:not([disabled])", $(".modal-card", modal));
  const closeModal = () => {
    if (!modal?.classList.contains("open")) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
    document.body.classList.remove("modal-open");
    opener?.focus();
  };
  $$(".detail-btn").forEach(btn => btn.addEventListener("click", () => {
    if (!modal) return;
    opener = btn;
    $("#modalTitle").textContent = btn.dataset.product;
    $("#modalText").textContent = texts[btn.dataset.product] || "Categoria demonstrativa.";
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    document.body.classList.add("modal-open");
    $(".modal-close", modal).focus();
  }));
  $$("[data-close-modal]").forEach(el => el.addEventListener("click", closeModal));
  document.addEventListener("keydown", e => {
    if (e.key === "Escape") { closeModal(); setMenu(false); }
    if (e.key === "Tab" && modal?.classList.contains("open")) {
      const f = focusables(), first = f[0], last = f[f.length - 1];
      if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
      else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
    }
  });

  // Revelação suave ao rolar
  if ("IntersectionObserver" in window) {
    const io = new IntersectionObserver(es => es.forEach(e => {
      if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
    }), { threshold: .12 });
    $$(".section").forEach(s => { s.classList.add("reveal"); io.observe(s); });
  }

  // Pré-reserva demonstrativa -> API Python/Java (/api/pre-reserva)
  const form = $("#reserveForm"), status = $("#formStatus");
  const say = (msg, ok) => { status.textContent = msg; status.className = "form-status " + (ok ? "ok" : "err"); };
  form?.addEventListener("submit", async e => {
    e.preventDefault();
    if (!form.checkValidity()) return form.reportValidity();
    if (!form.querySelector('input[name="pecas"]:checked')) return say("Escolha ao menos uma categoria de interesse.", false);
    const btn = form.querySelector("button[type=submit]");
    btn.disabled = true; say("Enviando…", true);
    try {
      const r = await fetch("/api/pre-reserva", { method: "POST", body: new URLSearchParams(new FormData(form)) });
      const j = await r.json().catch(() => ({}));
      if (r.ok) { form.reset(); say("Pré-reserva registrada. Nenhum pagamento foi solicitado.", true); }
      else say(j.erro === "validacao" ? "Confira os campos: " + (j.campos || []).join(", ") + "." : "Não foi possível registrar agora.", false);
    } catch {
      say("Servidor não encontrado. Abra a página por python backend/server.py ou pelo servidor Java.", false);
    } finally { btn.disabled = false; }
  });
})();
