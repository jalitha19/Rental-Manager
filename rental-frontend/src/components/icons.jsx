// A small, deliberately curated set of line icons (24x24, stroke-based)
// so the app doesn't need to pull in a full icon library for ~10 glyphs.

const base = {
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.75,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
}

export function IconDashboard(props) {
  return (
    <svg viewBox="0 0 24 24" {...base} {...props}>
      <rect x="3" y="3" width="7" height="9" rx="1.5" />
      <rect x="14" y="3" width="7" height="5" rx="1.5" />
      <rect x="14" y="12" width="7" height="9" rx="1.5" />
      <rect x="3" y="16" width="7" height="5" rx="1.5" />
    </svg>
  )
}

export function IconHouse(props) {
  return (
    <svg viewBox="0 0 24 24" {...base} {...props}>
      <path d="M4 11.5 12 4l8 7.5" />
      <path d="M6 10v9.5a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V10" />
      <path d="M10 20.5v-6h4v6" />
    </svg>
  )
}

export function IconDoor(props) {
  return (
    <svg viewBox="0 0 24 24" {...base} {...props}>
      <rect x="6" y="3" width="12" height="18" rx="1.5" />
      <circle cx="14.2" cy="12" r="0.9" fill="currentColor" stroke="none" />
    </svg>
  )
}

export function IconTenant(props) {
  return (
    <svg viewBox="0 0 24 24" {...base} {...props}>
      <circle cx="12" cy="8" r="3.4" />
      <path d="M5 20c1-3.6 4-5.5 7-5.5s6 1.9 7 5.5" />
    </svg>
  )
}

export function IconRentals(props) {
  return (
    <svg viewBox="0 0 24 24" {...base} {...props}>
      <path d="M7 3h10a1 1 0 0 1 1 1v16l-6-3.2L6 20V4a1 1 0 0 1 1-1Z" />
    </svg>
  )
}

export function IconPayments(props) {
  return (
    <svg viewBox="0 0 24 24" {...base} {...props}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7.5v9M9.5 15c.4 1 1.4 1.5 2.5 1.5s2.4-.6 2.4-1.8-1-1.6-2.4-1.9c-1.5-.3-2.5-.8-2.5-1.9S10.6 9 12 9c1.1 0 2 .5 2.4 1.4" />
    </svg>
  )
}

export function IconSettings(props) {
  return (
    <svg viewBox="0 0 24 24" {...base} {...props}>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 13.5a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.9 2.9l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 1 1-2.9-2.9l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.6-1h-.2a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.6-1.1 1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 1 1 2.9-2.9l.1.1a1.7 1.7 0 0 0 1.9.3h.1a1.7 1.7 0 0 0 1-1.6v-.2a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.6h.1a1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.9 2.9l-.1.1a1.7 1.7 0 0 0-.3 1.9v.1c.3.7.9 1.1 1.6 1.1h.2a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.6 1Z" />
    </svg>
  )
}

export function IconSearch(props) {
  return (
    <svg viewBox="0 0 24 24" {...base} {...props}>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </svg>
  )
}

export function IconLogout(props) {
  return (
    <svg viewBox="0 0 24 24" {...base} {...props}>
      <path d="M15 17.5V19a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h7a2 2 0 0 1 2 2v1.5" />
      <path d="M9 12h12m0 0-3.2-3.2M21 12l-3.2 3.2" />
    </svg>
  )
}

export function IconMenu(props) {
  return (
    <svg viewBox="0 0 24 24" {...base} {...props}>
      <path d="M4 7h16M4 12h16M4 17h16" />
    </svg>
  )
}

export function IconAlert(props) {
  return (
    <svg viewBox="0 0 24 24" {...base} {...props}>
      <path d="M12 3.5 21.5 20h-19L12 3.5Z" />
      <path d="M12 10v4M12 17h.01" />
    </svg>
  )
}

export function IconKey(props) {
  return (
    <svg viewBox="0 0 24 24" {...base} {...props}>
      <circle cx="8" cy="15" r="4.2" />
      <path d="M11 12.3 20 3.5M17 6.5l2 2M14.3 9.2l2 2" />
    </svg>
  )
}
