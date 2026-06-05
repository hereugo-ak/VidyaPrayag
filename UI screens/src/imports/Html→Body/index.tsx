import svgPaths from "./svg-r6tj9r2gtm";
import imgPlaceholderIllustration from "./0bb4ec7a9dd6fbd146e0d314ea089f87e768317a.png";

function AbstractCloudDecorationsSvg() {
  return (
    <div className="absolute h-[32px] left-[40px] top-[40px] w-[48px]" data-name="Abstract Cloud Decorations → SVG">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 48 32">
        <g clipPath="url(#clip0_1_115)" id="Abstract Cloud Decorations â SVG" opacity="0.3">
          <path d={svgPaths.p2a706a40} id="Vector" stroke="var(--stroke-0, white)" strokeWidth="2" />
        </g>
        <defs>
          <clipPath id="clip0_1_115">
            <rect fill="white" height="32" width="48" />
          </clipPath>
        </defs>
      </svg>
    </div>
  );
}

function Svg() {
  return (
    <div className="h-[32px] relative shrink-0 w-[48px]" data-name="SVG">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 48 32">
        <g clipPath="url(#clip0_1_109)" id="SVG">
          <path d={svgPaths.p2a706a40} id="Vector" stroke="var(--stroke-0, white)" strokeWidth="2" />
        </g>
        <defs>
          <clipPath id="clip0_1_109">
            <rect fill="white" height="32" width="48" />
          </clipPath>
        </defs>
      </svg>
    </div>
  );
}

function Container() {
  return (
    <div className="content-stretch flex flex-col items-start opacity-30 relative" data-name="Container">
      <Svg />
    </div>
  );
}

function PlaceholderIllustration() {
  return (
    <div className="flex-[1_0_0] min-h-px relative w-full" data-name="Placeholder illustration">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <img alt="" className="absolute left-0 max-w-none size-full top-0" src={imgPlaceholderIllustration} />
      </div>
    </div>
  );
}

function IllustrationPlaceholder() {
  return (
    <div className="content-stretch flex flex-col items-start justify-center relative shrink-0 size-[160px]" data-name="Illustration Placeholder">
      <PlaceholderIllustration />
    </div>
  );
}

function IllustrationPlaceholderMargin() {
  return (
    <div className="content-stretch flex flex-col h-[184px] items-start pb-[24px] relative shrink-0 w-[160px]" data-name="Illustration Placeholder:margin">
      <IllustrationPlaceholder />
    </div>
  );
}

function Heading() {
  return (
    <div className="content-stretch flex flex-col items-center relative shrink-0 w-full" data-name="Heading 1">
      <div className="[word-break:break-word] flex flex-col font-['Plus_Jakarta_Sans:Bold',sans-serif] font-bold justify-center leading-[0] relative shrink-0 text-[22px] text-center text-white whitespace-nowrap">
        <p>
          <span className="leading-[28px]">{`Welcome to VidyaSetu. `}</span>
          <span className="[word-break:break-word] font-['Noto_Color_Emoji:Regular',sans-serif] leading-[28px] not-italic">👋</span>
        </p>
      </div>
    </div>
  );
}

function Container2() {
  return (
    <div className="content-stretch flex flex-col items-center relative shrink-0 w-full" data-name="Container">
      <div className="[word-break:break-word] flex flex-col font-['Plus_Jakarta_Sans:Regular',sans-serif] font-normal justify-center leading-[0] relative shrink-0 text-[14px] text-[rgba(255,255,255,0.9)] text-center whitespace-nowrap">
        <p className="leading-[20px]">Bridging gaps for a glorious future</p>
      </div>
    </div>
  );
}

function Container1() {
  return (
    <div className="content-stretch flex flex-col gap-[8px] items-start relative shrink-0 w-[273.46px]" data-name="Container">
      <Heading />
      <Container2 />
    </div>
  );
}

function MobileTopHalfDesktopLeftHalfBrandIllustration() {
  return (
    <div className="bg-[#3cb9a9] h-[397.8px] relative shrink-0 w-full" data-name="Mobile Top Half / Desktop Left Half (Brand & Illustration)">
      <div className="flex flex-col items-center justify-center size-full">
        <div className="content-stretch flex flex-col items-center justify-center p-[24px] relative size-full">
          <AbstractCloudDecorationsSvg />
          <div className="absolute bottom-[84px] flex h-[24px] items-center justify-center right-[46px] w-[36px]">
            <div className="flex-none scale-x-75 scale-y-75">
              <Container />
            </div>
          </div>
          <IllustrationPlaceholderMargin />
          <Container1 />
        </div>
      </div>
    </div>
  );
}

function Button() {
  return (
    <div className="content-stretch flex flex-[1_0_0] flex-col items-center justify-center min-w-px py-[10px] relative" data-name="Button">
      <div className="[word-break:break-word] flex flex-col font-['Plus_Jakarta_Sans:Bold',sans-serif] font-bold justify-center leading-[0] relative shrink-0 text-[#006a60] text-[12px] text-center whitespace-nowrap">
        <p className="leading-[16px]">Parent</p>
      </div>
    </div>
  );
}

function Button1() {
  return (
    <div className="content-stretch flex flex-[1_0_0] flex-col items-center justify-center min-w-px py-[10px] relative" data-name="Button">
      <div className="[word-break:break-word] flex flex-col font-['Plus_Jakarta_Sans:SemiBold',sans-serif] font-semibold justify-center leading-[0] relative shrink-0 text-[#3d4947] text-[12px] text-center whitespace-nowrap">
        <p className="leading-[16px]">Admin</p>
      </div>
    </div>
  );
}

function Button2() {
  return (
    <div className="content-stretch flex flex-[1_0_0] flex-col items-center justify-center min-w-px py-[10px] relative" data-name="Button">
      <div className="[word-break:break-word] flex flex-col font-['Plus_Jakarta_Sans:SemiBold',sans-serif] font-semibold justify-center leading-[0] relative shrink-0 text-[#3d4947] text-[12px] text-center whitespace-nowrap">
        <p className="leading-[16px]">Teacher</p>
      </div>
    </div>
  );
}

function PortalSelectorTabs() {
  return (
    <div className="bg-[#f6f1ff] relative rounded-[12px] shrink-0 w-full" data-name="Portal Selector Tabs">
      <div className="flex flex-row justify-center size-full">
        <div className="content-stretch flex items-start justify-center p-[4px] relative size-full">
          <div className="absolute bg-white bottom-[4px] left-[4px] rounded-[8px] shadow-[0px_1px_2px_0px_rgba(0,0,0,0.05)] top-[4px] w-[112.66px]" data-name="Active Tab Background Indicator (Simulated with position absolute for smooth slide in real app)" />
          <Button />
          <Button1 />
          <Button2 />
        </div>
      </div>
    </div>
  );
}

function Label() {
  return (
    <div className="content-stretch flex flex-col items-start relative shrink-0 w-[346px]" data-name="Label">
      <div className="[word-break:break-word] flex flex-col font-['Plus_Jakarta_Sans:SemiBold',sans-serif] font-semibold justify-center leading-[0] relative shrink-0 text-[#3d4947] text-[12px] whitespace-nowrap">
        <p className="leading-[16px]">Email or Phone</p>
      </div>
    </div>
  );
}

function Container6() {
  return (
    <div className="content-stretch flex flex-[1_0_0] flex-col items-start min-w-px overflow-clip relative" data-name="Container">
      <div className="[word-break:break-word] flex flex-col font-['Plus_Jakarta_Sans:Regular',sans-serif] font-normal justify-center leading-[0] relative shrink-0 text-[#bcc9c6] text-[14px] w-full">
        <p className="leading-[normal]">Enter your credentials</p>
      </div>
    </div>
  );
}

function Input() {
  return (
    <div className="bg-[#f5f5f3] relative rounded-[12px] shrink-0 w-full" data-name="Input">
      <div className="flex flex-row justify-center overflow-clip rounded-[inherit] size-full">
        <div className="content-stretch flex items-start justify-center pl-[48px] pr-[16px] py-[15px] relative size-full">
          <Container6 />
        </div>
      </div>
    </div>
  );
}

function Container8() {
  return (
    <div className="h-[16px] relative shrink-0 w-[20px]" data-name="Container">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 20 16">
        <g id="Container">
          <path d={svgPaths.p13e73800} fill="var(--fill-0, #6D7A77)" id="Icon" />
        </g>
      </svg>
    </div>
  );
}

function Container7() {
  return (
    <div className="absolute bottom-0 content-stretch flex items-center left-0 pl-[16px] top-0" data-name="Container">
      <Container8 />
    </div>
  );
}

function Container5() {
  return (
    <div className="content-stretch flex flex-col items-start relative shrink-0 w-full" data-name="Container">
      <Input />
      <Container7 />
    </div>
  );
}

function Container4() {
  return (
    <div className="content-stretch flex flex-col gap-[4px] items-end relative shrink-0 w-full" data-name="Container">
      <Label />
      <Container5 />
    </div>
  );
}

function Label1() {
  return (
    <div className="content-stretch flex flex-col items-start relative shrink-0 w-[346px]" data-name="Label">
      <div className="[word-break:break-word] flex flex-col font-['Plus_Jakarta_Sans:SemiBold',sans-serif] font-semibold justify-center leading-[0] relative shrink-0 text-[#3d4947] text-[12px] whitespace-nowrap">
        <p className="leading-[16px]">Password</p>
      </div>
    </div>
  );
}

function Container11() {
  return (
    <div className="content-stretch flex flex-[1_0_0] flex-col items-start min-w-px overflow-clip relative" data-name="Container">
      <div className="[word-break:break-word] flex flex-col font-['Plus_Jakarta_Sans:Regular',sans-serif] font-normal justify-center leading-[0] relative shrink-0 text-[#bcc9c6] text-[14px] w-full">
        <p className="leading-[normal]">••••••••</p>
      </div>
    </div>
  );
}

function Input1() {
  return (
    <div className="bg-[#f5f5f3] relative rounded-[12px] shrink-0 w-full" data-name="Input">
      <div className="flex flex-row justify-center overflow-clip rounded-[inherit] size-full">
        <div className="content-stretch flex items-start justify-center px-[48px] py-[15px] relative size-full">
          <Container11 />
        </div>
      </div>
    </div>
  );
}

function Container13() {
  return (
    <div className="h-[21px] relative shrink-0 w-[16px]" data-name="Container">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16 21">
        <g id="Container">
          <path d={svgPaths.p12930f00} fill="var(--fill-0, #6D7A77)" id="Icon" />
        </g>
      </svg>
    </div>
  );
}

function Container12() {
  return (
    <div className="absolute bottom-0 content-stretch flex items-center left-0 pl-[16px] top-0" data-name="Container">
      <Container13 />
    </div>
  );
}

function Container14() {
  return (
    <div className="h-[19.8px] relative shrink-0 w-[22px]" data-name="Container">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 22 19.8">
        <g id="Container">
          <path d={svgPaths.p20809060} fill="var(--fill-0, #6D7A77)" id="Icon" />
        </g>
      </svg>
    </div>
  );
}

function Button3() {
  return (
    <div className="absolute bottom-[2.1px] content-stretch flex items-center pr-[16px] py-[12px] right-0 top-[2.1px]" data-name="Button">
      <Container14 />
    </div>
  );
}

function Container10() {
  return (
    <div className="content-stretch flex flex-col items-start relative shrink-0 w-full" data-name="Container">
      <Input1 />
      <Container12 />
      <Button3 />
    </div>
  );
}

function Link() {
  return (
    <div className="content-stretch flex flex-col items-start relative self-stretch shrink-0" data-name="Link">
      <div className="[word-break:break-word] flex flex-col font-['Plus_Jakarta_Sans:SemiBold',sans-serif] font-semibold justify-center leading-[0] relative shrink-0 text-[#006a60] text-[12px] whitespace-nowrap">
        <p className="leading-[16px]">Forgot Password?</p>
      </div>
    </div>
  );
}

function Container15() {
  return (
    <div className="content-stretch flex h-[20px] items-start justify-end pt-[4px] relative shrink-0 w-full" data-name="Container">
      <Link />
    </div>
  );
}

function Container9() {
  return (
    <div className="content-stretch flex flex-col gap-[4px] items-end pb-[16px] relative shrink-0 w-full" data-name="Container">
      <Label1 />
      <Container10 />
      <Container15 />
    </div>
  );
}

function Button4() {
  return (
    <div className="bg-[#26234d] content-stretch flex items-center justify-center py-[16px] relative rounded-[12px] shrink-0 w-full" data-name="Button">
      <div className="absolute bg-[rgba(255,255,255,0)] inset-0 rounded-[12px] shadow-[0px_4px_6px_-1px_rgba(0,0,0,0.1),0px_2px_4px_-2px_rgba(0,0,0,0.1)]" data-name="Button:shadow" />
      <div className="[word-break:break-word] flex flex-col font-['Plus_Jakarta_Sans:SemiBold',sans-serif] font-semibold justify-center leading-[0] relative shrink-0 text-[18px] text-center text-white whitespace-nowrap">
        <p className="leading-[24px]">Sign In</p>
      </div>
    </div>
  );
}

function Form() {
  return (
    <div className="content-stretch flex flex-col gap-[20px] items-start relative shrink-0 w-full" data-name="Form">
      <Container4 />
      <Container9 />
      <Button4 />
    </div>
  );
}

function Container17() {
  return (
    <div className="content-stretch flex flex-col items-start relative shrink-0" data-name="Container">
      <div className="[word-break:break-word] flex flex-col font-['Plus_Jakarta_Sans:Regular',sans-serif] font-normal justify-center leading-[0] relative shrink-0 text-[#3d4947] text-[14px] whitespace-nowrap">
        <p className="leading-[20px]">Not a member?</p>
      </div>
    </div>
  );
}

function LinkMargin() {
  return (
    <div className="content-stretch flex flex-col items-start pl-[8px] relative shrink-0" data-name="Link:margin">
      <div className="[word-break:break-word] flex flex-col font-['Plus_Jakarta_Sans:SemiBold',sans-serif] font-semibold justify-center leading-[0] relative shrink-0 text-[#9e421a] text-[14px] whitespace-nowrap">
        <p className="leading-[20px]">Register Now</p>
      </div>
    </div>
  );
}

function Container16() {
  return (
    <div className="content-stretch flex items-center justify-center pb-[24px] relative shrink-0 w-full" data-name="Container">
      <Container17 />
      <LinkMargin />
    </div>
  );
}

function Container3() {
  return (
    <div className="content-stretch flex flex-col gap-[32px] items-start max-w-[448px] relative shrink-0 w-full" data-name="Container">
      <PortalSelectorTabs />
      <Form />
      <Container16 />
    </div>
  );
}

function MobileBottomHalfDesktopRightHalfLoginForm() {
  return (
    <div className="absolute bg-[#fcf8ff] content-stretch flex flex-col items-start justify-center left-0 overflow-auto pb-[47.09px] pt-[47.11px] px-[20px] right-0 rounded-tl-[32px] rounded-tr-[32px] shadow-[0px_-8px_30px_0px_rgba(0,0,0,0.05)] top-[-32px]" data-name="Mobile Bottom Half / Desktop Right Half (Login Form)">
      <Container3 />
    </div>
  );
}

function MobileBottomHalfDesktopRightHalfLoginFormMargin() {
  return (
    <div className="h-[486.2px] relative shrink-0 w-full" data-name="Mobile Bottom Half / Desktop Right Half (Login Form):margin">
      <MobileBottomHalfDesktopRightHalfLoginForm />
    </div>
  );
}

export default function HtmlBody() {
  return (
    <div className="content-stretch flex flex-col items-start relative size-full" style={{ backgroundImage: "linear-gradient(90deg, rgb(252, 248, 255) 0%, rgb(252, 248, 255) 100%), linear-gradient(90deg, rgb(255, 255, 255) 0%, rgb(255, 255, 255) 100%)" }} data-name="Html → Body">
      <MobileTopHalfDesktopLeftHalfBrandIllustration />
      <MobileBottomHalfDesktopRightHalfLoginFormMargin />
    </div>
  );
}