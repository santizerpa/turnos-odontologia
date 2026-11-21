# Registro de evidencia y accesos

**Fecha de corte:** 2026-11-21. Todos los accesos se hicieron ese día, desde una sesión con acceso web restringido a `webfetch` sobre HTTPS.

**Método:** lectura de páginas públicas de proveedor (home, planes/precios, funcionalidades, FAQ, datos estructurados JSON-LD). No se accedió a demos, pruebas gratuitas, documentación interna ni bases de clientes. Por lo tanto, "comprobado" significa *documentado públicamente por el proveedor en esa fecha*, no verificado en operación real.

## 1. Evidencia por sistema

| Sistema | URL consultada | Secciones leídas | Quedó `No evidenciado` |
|---|---|---|---|
| DentalSoft | https://www.dentalsoft.com.ar/ | Home, funcionalidades, planes, FAQ, privacidad | Facturación AFIP/ARCA; lista de espera; score de riesgo; cantidad de clientes |
| Dentalink | https://web-ar.softwaredentalink.com/planes | Planes AR (Esencial/Pro/Titanium), funcionalidades, FAQ | Precio; Mercado Pago; ARCA; lista de espera; seña online |
| AgendaPro | https://agendapro.com/ar/planes | Planes, add-ons de IA, funcionalidades | Odontograma; facturación ARCA operativa ("Próximamente"); cantidad de clientes |
| Reservo | https://reservo.com.ar/precios/ | Precios/cotización, módulos, selector de país | Precio; WhatsApp; odontograma; Mercado Pago; ARCA; seguridad y exportación |
| Doctoralia Pro | https://pro.doctoralia.com/ar/precio | Planes Starter/Plus/VIP, comparativa, FAQ | WhatsApp; Mercado Pago; odontograma; caja y facturación fiscal |
| Doctocliq | https://www.doctocliq.com/planes-y-precios | Planes, features por plan, add-ons, selector de país | Presencia en Argentina; Mercado Pago; ARCA; cantidad de clientes |
| Citalo | https://www.citalo.app/ar/precios | Planes, funcionalidades, página odontológica | Odontograma; certificación de seguridad; exportación; ARCA |
| Dentidesk | https://www.dentidesk.com/ y https://www.dentidesk.com/dentidesk/feature | Home, características (agenda, pacientes, administrador, reportes, configuración) | Precio; WhatsApp; facturación electrónica al paciente; cantidad de clientes |
| OdontoApp | https://odontoapp.com.ar/ (HTML y JSON-LD) | Home estática, `SoftwareApplication`, `FAQPage`, países | Detalle por plan; reserva online; Mercado Pago; ARCA; lista de espera; clientes reales |
| Nubimed | https://www.nubimed.com/planes-precios/ | Planes LITE/PREMIUM/MULTICENTRO, navegación dental y WhatsApp | Presencia argentina; Mercado Pago; ARCA; videoconsulta; cantidad de clientes |
| Clinic Cloud | https://clinic-cloud.com/tarifas | Tarifas, comparativa de planes, módulos | Integración argentina; precio para Argentina; clientes verificables |
| Dentally | https://www.dentally.com/en-gb/pricing | Precios por sillón, comparativa de planes, navegación de producto | Precio sin sillón; WhatsApp; integración argentina; cantidad de clientes |
| Feegow | https://feegowclinic.com.br/precos-e-planos | Planes Starter/Plus/VIP, home, certificaciones | Odontograma; WhatsApp; integración argentina; cantidad de clientes |
| iClinic | https://iclinic.com.br/precos/ | Planes, comparativa, add-ons, FAQ | Odontograma; integración argentina; ICP-Brasil; clientes verificables |
| Open Dental | https://www.opendental.com/ | Home, listado de features, navegación | Precio; modelo de despliegue; recordatorios; agendamiento online; integración argentina |
| SimplyBook.me | https://simplybook.me/en/pricing | Planes Free/Basic/Standard/Premium, features por plan | WhatsApp; ficha clínica; caja fiscal AR; cantidad de clientes |
| Calendly | https://calendly.com/pricing | Planes, comparativa, seguridad | Función clínica; caja/facturación; integración argentina; cantidad de clientes |

## 2. Fallas de acceso y URLs descartadas

| URL | Resultado | Tratamiento |
|---|---|---|
| https://www.dentalink.com/ | Error de transporte (dominio inexistente o sin DNS) | Se usó el dominio oficial `softwaredentalink.com` y el sitio argentino `web-ar.softwaredentalink.com`. |
| https://www.clinicpoint.com.ar/ | Error de transporte | Candidato no verificable; excluido de la matriz. |
| https://www.odontonet.com.ar/ | Error de transporte | Candidato no verificable; excluido de la matriz. |
| https://www.dentaloffice.com.ar/ | Error de transporte | Candidato no verificable; excluido de la matriz. |
| https://www.dentaly.com.ar/ | Respuesta vacía en dos intentos | `No evidenciado`; no se puntúa. |
| https://agendapro.com/ar/precios | HTTP 404 | Se usó `https://agendapro.com/ar/planes`. |
| https://www.cliniccloud.com/precios/ | HTTP 404 | Se usó `https://clinic-cloud.com/tarifas`. |
| https://odontoapp.com.ar/precios | HTTP 404 | El precio está en los datos estructurados de la home; se documentó así. |
| https://pro.doctoralia.com/ar/precios | HTTP 404 | Se usó `https://pro.doctoralia.com/ar/precio`. |
| https://www.fresha.com/pricing | Respuesta vacía | No puntuado; pendiente. |
| https://www.fresha.com/for-business | Respuesta vacía | No puntuado; pendiente. |

## 3. Pendientes explícitos (no investigados en esta ronda)

Dentaltap, Dentrix y Curve Dental no se consultaron dentro del presupuesto de esta investigación. Fresha quedó inaccesible. No se completaron datos por inferencia ni se usaron sitios de comparación de terceros como evidencia.

## 4. Correcciones aplicadas (2026-11-21)

| Ítem | Valor anterior | Valor corregido | Fuente de verificación |
|---|---|---|---|
| DentalSoft precio | ARS 30.000/60.000 presentado como fijo | Se conserva el precio, aclarado como base para 1 profesional; fuente secundaria Odonthia coincide, pero el sitio oficial no publica una sección de precios clara | https://www.dentalsoft.com.ar/ ; https://odonthia.com/blog/mejores-software-odontologicos-argentina-2026 |
| AgendaPro IVA | ARS 9.900/19.900/39.900/129.900 + IVA | IVA incluido | https://agendapro.com/ar/planes |
| AgendaPro origen | Chile declarado | País de origen no evidenciado en la página argentina | https://agendapro.com/ar/planes |
| Doctoralia web profesional | Web ARS 4.000 atribuida como costo confirmado | El plan muestra 4.000 ARS como costo adicional en el comparador; no se presenta como factura separada confiable, ni alta/costo mensual completo | https://pro.doctoralia.com/ar/precio |
| Citalo odontograma | Sin odontograma evidenciado | Odontograma digital comprobado | https://www.citalo.app/ar/software-turnos-odontologos |
| OdontoApp reserva | Reserva online no evidenciada | Reserva online comprobada en Todos los planes | https://odontoapp.com.ar/ |
| Nubimed usuarios | EUR 27,40/50,10 sin costos adicionales | 1 usuario con agenda incluido; +EUR 2,40/+5,10 por usuario adicional con agenda (+IVA); recepción/admin sin agenda ilimitados y gratis | https://www.nubimed.com/planes-precios/ |
| Clinic Cloud precio | EUR 29/49/79 + IVA | EUR 49/79/109 + IVA (Mini/Pro/Max); descuento Black Friday excluido | https://clinic-cloud.com/tarifas |
| iClinic público | Médico/consultorio/red | Consultorios y clínicas pequeñas; para redes/múltiples unidades remite a Shosp | https://iclinic.com.br/precos/ |
| Calendly precio | USD 0/10/16 por asiento | La página live consultada sigue mostrando Free USD 0, Standard USD 10 y Teams USD 16; Enterprise desde USD 15.000/año (50 asientos) | https://calendly.com/pricing |
| Campos faltantes | Operación/seguridad/diferenciales incompletos | Agregada tabla A2 por 17 sistemas; faltantes marcados `No evidenciado` | URLs oficiales listadas en fuentes.md |

## 5. Advertencia sobre cifras de adopción

Las cantidades de clientes, profesionales o países que aparecen en la sección A son **afirmaciones del proveedor** y se marcan como tales. No se usaron para calcular el puntaje de "Precio y adopción" salvo como señal cualitativa de presencia comercial, y nunca se presentan como dato auditado.
