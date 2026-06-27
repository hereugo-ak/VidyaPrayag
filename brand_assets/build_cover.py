import cairosvg

# ===== PREMIUM COVER / BACKGROUND (1920x1080) =====
# Light lavender base, large faint bridge watermark on the right,
# small crisp logo mark + wordmark top-left, premium tagline center-left.
BG_SVG = '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1920 1080">
  <defs>
    <linearGradient id="base" x1="0" y1="0" x2="1920" y2="1080" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#FBFAFF"/>
      <stop offset="0.5" stop-color="#F0EEFC"/>
      <stop offset="1" stop-color="#E6E6FA"/>
    </linearGradient>
    <linearGradient id="span" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="#544AB8"/>
      <stop offset="0.5" stop-color="#6C5CE0"/>
      <stop offset="1" stop-color="#544AB8"/>
    </linearGradient>
    <linearGradient id="head" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="#26234D"/>
      <stop offset="1" stop-color="#544AB8"/>
    </linearGradient>
  </defs>

  <rect width="1920" height="1080" fill="url(#base)"/>

  <!-- soft accent glows -->
  <circle cx="1620" cy="240" r="420" fill="#6C5CE0" opacity="0.06"/>
  <circle cx="240" cy="940" r="360" fill="#544AB8" opacity="0.05"/>

  <!-- ===== large faint watermark bridge (right side) ===== -->
  <g transform="translate(1180,300) scale(13)" opacity="0.12">
    <path d="M16 36 Q32 16 48 36" stroke="#6C5CE0" stroke-width="3.4" stroke-linecap="round" fill="none"/>
    <path d="M14 44 H50" stroke="#26234D" stroke-width="4" stroke-linecap="round"/>
    <path d="M22 36 V44" stroke="#26234D" stroke-width="1.8" stroke-linecap="round"/>
    <path d="M32 26 V44" stroke="#26234D" stroke-width="1.8" stroke-linecap="round"/>
    <path d="M42 36 V44" stroke="#26234D" stroke-width="1.8" stroke-linecap="round"/>
    <circle cx="16" cy="36" r="2.9" fill="#26234D"/>
    <circle cx="48" cy="36" r="2.9" fill="#26234D"/>
    <circle cx="32" cy="26" r="3" fill="#6C5CE0"/>
  </g>

  <!-- ===== crisp brand lockup top-left ===== -->
  <g transform="translate(150,120) scale(1.9)">
    <rect x="0" y="0" width="64" height="64" rx="18" fill="#FFFFFF"/>
    <rect x="0.5" y="0.5" width="63" height="63" rx="17.5" stroke="#26234D" stroke-opacity="0.08"/>
    <path d="M16 36 Q32 16 48 36" stroke="url(#span)" stroke-width="3.4" stroke-linecap="round" fill="none"/>
    <path d="M14 44 H50" stroke="#26234D" stroke-width="4" stroke-linecap="round"/>
    <path d="M22 36 V44" stroke="#26234D" stroke-opacity="0.78" stroke-width="1.8" stroke-linecap="round"/>
    <path d="M32 26 V44" stroke="#26234D" stroke-opacity="0.78" stroke-width="1.8" stroke-linecap="round"/>
    <path d="M42 36 V44" stroke="#26234D" stroke-opacity="0.78" stroke-width="1.8" stroke-linecap="round"/>
    <circle cx="16" cy="36" r="2.9" fill="#26234D"/>
    <circle cx="48" cy="36" r="2.9" fill="#26234D"/>
    <circle cx="32" cy="26" r="3" fill="#6C5CE0"/>
  </g>
  <text x="290" y="190" font-family="Arial, Helvetica, sans-serif" font-weight="800"
        font-size="64" letter-spacing="-2" fill="#26234D">Enroll<tspan fill="#6C5CE0">+</tspan></text>

  <!-- ===== tagline block ===== -->
  <text x="150" y="540" font-family="Arial, Helvetica, sans-serif" font-weight="800"
        font-size="130" letter-spacing="-4" fill="url(#head)">The OS for</text>
  <text x="150" y="690" font-family="Arial, Helvetica, sans-serif" font-weight="800"
        font-size="130" letter-spacing="-4" fill="url(#head)">your campus.</text>

  <text x="156" y="790" font-family="Arial, Helvetica, sans-serif" font-weight="500"
        font-size="44" letter-spacing="0" fill="#544AB8">One platform connecting your office, your teachers, and every parent.</text>

  <!-- accent underline -->
  <rect x="156" y="826" width="220" height="8" rx="4" fill="#6C5CE0"/>
</svg>'''

cairosvg.svg2png(bytestring=BG_SVG.encode(), write_to="brand_assets/enrollplus-cover-bg.png",
                 output_width=3840, output_height=2160)
print("cover -> brand_assets/enrollplus-cover-bg.png (3840x2160)")
