// Preparar la foto de una etiqueta antes de mandarla: más chica y en JPG.

/** Lado más largo, en píxeles. Alcanza para leer el número y baja una foto de celular de ~4 MB a ~200 KB. */
const LADO_MAXIMO = 1600

/**
 * Reduce la foto en el propio navegador (con un canvas, un "lienzo" de dibujo).
 * Si el navegador no puede (o la foto ya es chica), regresa el archivo original.
 */
export async function reducirFoto(archivo: File): Promise<Blob> {
  if (typeof createImageBitmap !== 'function') {
    return archivo
  }
  try {
    const imagen = await createImageBitmap(archivo)
    const escala = Math.min(1, LADO_MAXIMO / Math.max(imagen.width, imagen.height))
    const lienzo = document.createElement('canvas')
    lienzo.width = Math.round(imagen.width * escala)
    lienzo.height = Math.round(imagen.height * escala)
    lienzo.getContext('2d')?.drawImage(imagen, 0, 0, lienzo.width, lienzo.height)
    imagen.close()
    const reducida = await new Promise<Blob | null>((resolver) => lienzo.toBlob(resolver, 'image/jpeg', 0.85))
    return reducida && reducida.size < archivo.size ? reducida : archivo
  } catch {
    // Formatos que el navegador no sabe abrir (ej. HEIC en algunos): se manda tal cual.
    return archivo
  }
}
