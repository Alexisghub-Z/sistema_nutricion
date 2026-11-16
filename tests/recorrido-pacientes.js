async page => {
    const base = 'http://localhost:8080/sistema_nutricion-war';
    const results = [];
    const check = (name, ok) => { if (!ok) throw new Error(name); results.push(name); };
    await page.context().clearCookies();
    await page.goto(base + '/vistas/pacientes/lista.xhtml');
    const loginMessages = page.locator('[id="frmAcceso:msgs"]');
    check('Acceso privado redirige al login', page.url().endsWith('/acceso.xhtml'));
    await page.getByRole('button', { name: 'Entrar', exact: true }).click();
    await loginMessages.getByText('Escribe tu correo.', { exact: true }).waitFor();
    check('Login valida obligatorios', await loginMessages.getByText('Escribe tu contraseña.', { exact: true }).isVisible());
    await page.getByRole('textbox', { name: 'Correo*', exact: true }).fill('admin@nutricion.com');
    await page.getByRole('textbox', { name: 'Contraseña*', exact: true }).fill('incorrecta-qa');
    await page.getByRole('button', { name: 'Entrar', exact: true }).click();
    await loginMessages.getByText('Correo o contraseña incorrectos.', { exact: true }).waitFor();
    check('Login rechaza contraseña incorrecta', page.url().endsWith('/acceso.xhtml'));
    await page.getByRole('textbox', { name: 'Contraseña*', exact: true }).fill('admin123');
    await page.getByRole('button', { name: 'Entrar', exact: true }).click();
    await page.waitForURL('**/vistas/dashboard.xhtml');
    check('Login correcto carga panel', await page.getByRole('heading', { name: 'Próximas citas' }).isVisible());
    await page.goto(base + '/vistas/pacientes/lista.xhtml');
    await page.getByRole('button', { name: 'Nuevo paciente', exact: true }).click();
    await page.getByRole('dialog').waitFor();
    check('Sin avisos de botones fuera de formulario', !(await page.locator('[id="frmDialogo:msgs"]').innerText()).includes('Form in its ancestry'));
    const dialog = page.getByRole('dialog', { name: 'Paciente', exact: true });
    const form = page.locator('[id="frmDialogo"]');
    const messages = page.locator('[id="frmDialogo:msgs"]');
    const email = 'u4.qa.' + Date.now() + '@example.invalid';
    const phone = '990' + String(Date.now()).slice(-7);
    const field = name => page.locator('[id="frmDialogo:' + name + '"]');
    const save = async () => {
        await field('nombre').click(); // Cierra el calendario antes de pulsar el botón.
        await form.getByRole('button', { name: 'Guardar', exact: true }).click();
    };
    const row = () => page.getByRole('row').filter({ hasText: email });
    const filter = async () => {
        const search = page.getByRole('textbox', { name: 'Buscar por nombre, correo o teléfono' });
        await search.fill(email); await search.press('End');
        await page.waitForResponse(r => r.request().method() === 'POST' && r.url().includes('/pacientes/lista.xhtml'));
    };
    const login = async () => {
        await page.goto(base + '/acceso.xhtml');
        await page.getByRole('textbox', { name: 'Correo*', exact: true }).fill('admin@nutricion.com');
        await page.getByRole('textbox', { name: 'Contraseña*', exact: true }).fill('admin123');
        await page.getByRole('button', { name: 'Entrar', exact: true }).click();
        await page.waitForURL('**/vistas/dashboard.xhtml');
    };
    let created = false;
    try {
        await save();
        await messages.getByText('Escribe el nombre.', { exact: true }).waitFor();
        check('Paciente valida obligatorios en JSF', await messages.getByText('Escribe el correo.', { exact: true }).isVisible());
        await field('nombre').fill('U4 QA temporal');
        await field('email').fill('sin-correo');
        await field('tel').fill(phone);
        await field('nac_input').fill('01/01/2000');
        await save();
        await messages.getByText('Escribe un correo válido, por ejemplo nombre@correo.com.', { exact: true }).waitFor();
        check('Correo inválido no cierra formulario', await dialog.isVisible());
        await field('nombre').click();
        await page.locator('#ui-datepicker-div').waitFor({state:'hidden'});
        await dialog.screenshot({path:'.cache/evidencias-u4/validacion-correo.png'});
        await field('email').fill(email);
        await field('tel').fill('123');
        await save();
        await messages.getByText('El teléfono debe tener entre 10 y 15 dígitos, con + inicial opcional.', { exact: true }).waitFor();
        check('Fachada rechaza teléfono corto', await dialog.isVisible());
        await field('tel').fill(phone);
        await field('nac_input').fill('01/01/2099');
        await save();
        await messages.getByText('La fecha de nacimiento no puede ser futura.', { exact: true }).waitFor();
        check('Fachada rechaza nacimiento futuro', await dialog.isVisible());
        await field('nac_input').fill('01/01/2000');
        await save();
        await dialog.waitFor({state:'hidden'});
        created = true;
        await filter(); await row().waitFor();
        check('Alta aparece en tabla', await row().count() === 1);
        await page.screenshot({path:'.cache/evidencias-u4/paciente-ficticio.png', fullPage:true});
        await page.getByRole('button', {name:'Nuevo paciente', exact:true}).click();
        await dialog.waitFor();
        await field('nombre').fill('U4 QA duplicado');
        await field('email').fill(email);
        await field('tel').fill('991' + phone.slice(3));
        await field('nac_input').fill('01/01/2000');
        await save();
        await messages.getByText('Ya existe un paciente con el correo ' + email + '.', {exact:true}).waitFor();
        check('Correo duplicado se rechaza', await dialog.isVisible());
        await field('email').fill('otro-correo-invalido');
        await save();
        await messages.getByText('Escribe un correo válido, por ejemplo nombre@correo.com.', { exact: true }).waitFor();
        await form.getByRole('button', {name:'Cancelar', exact:true}).click();
        await page.getByRole('button', {name:'Nuevo paciente', exact:true}).click();
        await dialog.waitFor();
        check('Nuevo formulario descarta valores cancelados', await field('nombre').inputValue() === '' && await field('email').inputValue() === '');
        await form.getByRole('button', {name:'Cancelar', exact:true}).click();
        await row().getByTitle('Editar', {exact:true}).click();
        await dialog.waitFor();
        await field('alergias').fill('Marca ficticia U4 QA');
        await save(); await dialog.waitFor({state:'hidden'});
        await page.reload(); await filter();
        await row().getByText('Marca ficticia U4 QA', {exact:true}).waitFor();
        check('Edición persiste tras recargar', true);
        await page.getByRole('button', {name:'Salir', exact:true}).click();
        await page.waitForURL('**/acceso.xhtml');
        await page.goto(base + '/vistas/pacientes/lista.xhtml');
        check('Logout retira acceso privado', page.url().endsWith('/acceso.xhtml'));
        await login(); await page.goto(base + '/vistas/pacientes/lista.xhtml');
        await filter(); await row().waitFor();
        check('Registro persiste tras nueva sesión', await row().count() === 1);
        await row().getByTitle('Eliminar', {exact:true}).click();
        await page.getByRole('button', {name:'No', exact:true}).click();
        check('Cancelar eliminación conserva registro', await row().count() === 1);
        await row().getByTitle('Eliminar', {exact:true}).click();
        await page.getByRole('button', {name:'Sí', exact:true}).click();
        await row().waitFor({state:'detached'});
        created = false;
        check('Elimina solo el paciente ficticio', await row().count() === 0);
        await page.getByRole('button', {name:'Salir', exact:true}).click();
        return {results, total:results.length, emailTemporal:email, limpieza:'registro eliminado'};
    } finally {
        if (created) {
            await login(); await page.goto(base + '/vistas/pacientes/lista.xhtml');
            await filter();
            if (await row().count() === 1) {
                await row().getByTitle('Eliminar', {exact:true}).click();
                await page.getByRole('button', {name:'Sí', exact:true}).click();
                await row().waitFor({state:'detached'});
            }
        }
    }
}
