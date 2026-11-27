(function () {
    var codigo = document.getElementById('frmRestablecer:codigo');
    var token = new URLSearchParams(window.location.hash.slice(1)).get('codigo');
    if (codigo && token && /^[A-Za-z0-9_-]{43}$/.test(token)) {
        codigo.value = token;
        window.history.replaceState(null, '', window.location.pathname);
    }
}());
