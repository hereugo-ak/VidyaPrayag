// ═══════════════════════════════════════════════════════════════════════
// Enroll+ People Tab — Premium Prototype JS
// ═══════════════════════════════════════════════════════════════════════

function showView(id) {
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  const el = document.getElementById(id);
  if (el) {
    el.classList.add('active');
    const screen = document.querySelector('.screen');
    if (screen) screen.scrollTop = 0;
  }
}

function switchSubTab(tab) {
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  document.querySelector(`.tab[data-subtab="${tab}"]`).classList.add('active');
  document.querySelectorAll('.subtab-content').forEach(c => c.classList.add('hidden'));
  document.getElementById('subtab-' + tab).classList.remove('hidden');
}

function openSheet(id) {
  document.getElementById(id).classList.add('active');
  document.getElementById('overlay-bg').classList.add('active');
}

function closeSheet() {
  document.querySelectorAll('.sheet').forEach(s => s.classList.remove('active'));
  document.getElementById('overlay-bg').classList.remove('active');
}

function openConfirm(id) {
  document.getElementById(id).classList.add('active');
}

function closeConfirm(id) {
  document.getElementById(id).classList.remove('active');
}

function toggleDropdown(id) {
  const el = document.getElementById(id + '-dropdown');
  if (el) el.classList.toggle('hidden');
}

function closeDropdown(id) {
  const el = document.getElementById(id + '-dropdown');
  if (el) el.classList.add('hidden');
}

function toggleChip(el) {
  el.classList.toggle('selected');
}

// Close dropdowns on outside click
document.addEventListener('click', function(e) {
  if (!e.target.closest('.btn-more') && !e.target.closest('.dropdown')) {
    document.querySelectorAll('.dropdown').forEach(d => d.classList.add('hidden'));
  }
});
