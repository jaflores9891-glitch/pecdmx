// Números y dinero como se escriben en México.

const PESOS = new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' })
const KILOS = new Intl.NumberFormat('es-MX', { maximumFractionDigits: 2 })

/** 2705 → "$2,705.00" */
export function pesos(monto: number): string {
  return PESOS.format(monto)
}

/** 4.8 → "4.8 kg" */
export function kilos(peso: number): string {
  return `${KILOS.format(peso)} kg`
}
