(function () {
    var ultimaSolicitud = 0;

    function actualizar() {
        var boton = document.querySelector('.js-actualizar-al-volver');
        if (boton && !document.hidden && Date.now() - ultimaSolicitud > 500) {
            ultimaSolicitud = Date.now();
            boton.click();
        }
    }

    document.addEventListener('visibilitychange', actualizar);
    window.addEventListener('focus', actualizar);
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) {
            actualizar();
        }
    });
}());
