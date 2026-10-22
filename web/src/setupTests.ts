import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Agrega a los tests verificaciones como toBeInTheDocument() o toHaveTextContent().
import '@testing-library/jest-dom/vitest'

// Borra la página pintada al terminar cada test, para que el siguiente empiece con el "pizarrón" limpio.
afterEach(() => {
  cleanup()
})
