# OdontoApp (Sovil) — notas de investigación

- **Proveedor:** Sovil (Argentina).
- **URL oficial:** https://odontoapp.com.ar/
- **Fecha de consulta:** 2026-11-21
- **Secciones leídas:** home (HTML estático y datos estructurados JSON-LD), FAQ, metadata, listado de países.
- **Nivel de evidencia:** funcionalidad y precio comprobados en datos estructurados y FAQ del sitio oficial. El cuerpo renderizado por JavaScript no fue legible, pero el HTML incluye `SoftwareApplication` y `FAQPage` con precios y features.

## Datos verificados

- Software web para consultorios odontológicos; declara disponibilidad en **19 países de Latinoamérica**, incluida Argentina.
- **Precio publicado en datos estructurados:** `AggregateOffer` en ARS con `lowPrice 0`, `highPrice 48.500`, `offerCount 3`.
- FAQ: **plan Gratuito para siempre (hasta 10 pacientes)** y planes pagos **desde AR$36.000 por mes** en Argentina (USD 20 en el resto de Latinoamérica). **14 días de prueba** sin tarjeta.
- Features declaradas: historia clínica digital, **odontograma de 32 dientes**, **periodontograma con gráfico visual**, agenda diaria/semanal/mensual, **recordatorios automáticos por WhatsApp**, radiografías e imágenes en la nube, facturación y exportación a Excel, dashboard de producción, múltiples sedes, almacenamiento en la nube.
- FAQ: los recordatorios de WhatsApp se envían automáticamente al agendar, sin apps externas.
- Declara cumplimiento de la **Ley 25.326** de Protección de Datos Personales de Argentina.

## Citas literales

> "Menos inasistencias en tu consultorio odontológico. Más tiempo para atender."
> "Más de 100 odontólogos y clínicas ya gestionan su consultorio con OdontoApp."
> "Hay un plan Gratuito para siempre (hasta 10 pacientes) y planes pagos desde AR$36.000 por mes en Argentina."

## No evidenciado

- Detalle de cada plan y qué incluye el tope de ARS 48.500.
- Reserva online de turnos por el paciente (no figura en `featureList`).
- Mercado Pago, señas o cobro online.
- Facturación electrónica AFIP/ARCA (solo "facturación" genérica).
- Lista de espera, score de riesgo o reasignación automática del hueco.
- Cantidad real de clientes (la cifra es afirmación del proveedor).

## Nota crítica

Es el competidor local más directo y el único que ya ocupa el mensaje "menos inasistencias" con precio en pesos y WhatsApp. Obliga a diferenciar por mecanismo (riesgo, lista de espera, reasignación), no por promesa.
