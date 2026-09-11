import Modal from './Modal'

export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirm',
  danger = false,
  busy = false,
  onConfirm,
  onClose,
}) {
  return (
    <Modal open={open} title={title} onClose={onClose}>
      <p className="text-sm text-ink-soft">{message}</p>
      <div className="mt-6 flex justify-end gap-2">
        <button
          type="button"
          onClick={onClose}
          className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-ink-soft hover:bg-ink/5"
        >
          Cancel
        </button>
        <button
          type="button"
          disabled={busy}
          onClick={onConfirm}
          className={`rounded-lg px-4 py-2 text-sm font-semibold text-white disabled:opacity-60 ${
            danger ? 'bg-danger hover:bg-danger/90' : 'bg-brand hover:bg-brand-light'
          }`}
        >
          {busy ? 'Please wait…' : confirmLabel}
        </button>
      </div>
    </Modal>
  )
}
