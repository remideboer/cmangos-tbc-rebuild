-- Hero (classless): PlayerFrameManaBar → three segments (mana / rage / energy).
-- Values come from server LANG_ADDON PowerUpdate (UnitRage/UnitEnergy stay empty on 8606).
-- Stock FrameXML untouched — remove this AddOn to restore.

local ADDON = "HeroPowerBars"
local PREFIX = "HeroPowerBars"

local frame = CreateFrame("Frame", ADDON .. "EventFrame")
local active = false
local segments = {}
local cache = {
    [0] = { cur = 0, max = 1 },
    [1] = { cur = 0, max = 1 },
    [3] = { cur = 0, max = 1 },
}
local POWER_ORDER = { 0, 1, 3 }

local function colorFor(powerType)
    local c = ManaBarColor and ManaBarColor[powerType]
    if c then
        return c.r, c.g, c.b
    end
    if powerType == 1 then
        return 1.0, 0.0, 0.0
    end
    if powerType == 3 then
        return 1.0, 1.0, 0.0
    end
    return 0.0, 0.0, 1.0
end

local function hideStockManaVisual()
    local bar = PlayerFrameManaBar
    if not bar then
        return
    end
    local tex = bar:GetStatusBarTexture()
    if tex then
        tex:SetAlpha(0)
    end
    if PlayerFrameManaBarText then
        PlayerFrameManaBarText:Hide()
    end
end

local function restoreStockManaVisual()
    local bar = PlayerFrameManaBar
    if not bar then
        return
    end
    local tex = bar:GetStatusBarTexture()
    if tex then
        tex:SetAlpha(1)
    end
    if PlayerFrameManaBarText then
        PlayerFrameManaBarText:Show()
    end
end

local function layoutSegments()
    local parent = PlayerFrameManaBar
    if not parent or not segments[1] then
        return
    end
    local w = parent:GetWidth()
    local h = parent:GetHeight()
    if not w or w < 3 then
        return
    end
    local gap = 1
    local segW = (w - gap * 2) / 3
    for i = 1, 3 do
        local seg = segments[i]
        seg:ClearAllPoints()
        seg:SetWidth(segW)
        seg:SetHeight(h)
        if i == 1 then
            seg:SetPoint("LEFT", parent, "LEFT", 0, 0)
        else
            seg:SetPoint("LEFT", segments[i - 1], "RIGHT", gap, 0)
        end
    end
end

local function ensureSegments()
    local parent = PlayerFrameManaBar
    if not parent then
        return false
    end
    if segments[1] then
        return true
    end
    for i = 1, 3 do
        local powerType = POWER_ORDER[i]
        local seg = CreateFrame("StatusBar", ADDON .. "Seg" .. i, parent)
        seg:SetFrameLevel(parent:GetFrameLevel() + 2)
        seg:SetStatusBarTexture("Interface\\TargetingFrame\\UI-StatusBar")
        local r, g, b = colorFor(powerType)
        seg:SetStatusBarColor(r, g, b)
        local bg = seg:CreateTexture(nil, "BACKGROUND")
        bg:SetAllPoints(seg)
        bg:SetTexture("Interface\\TargetingFrame\\UI-StatusBar")
        bg:SetVertexColor(0, 0, 0, 0.55)
        local text = seg:CreateFontString(nil, "OVERLAY", "TextStatusBarText")
        text:SetPoint("CENTER", seg, "CENTER", 0, 0)
        seg.text = text
        seg.powerType = powerType
        seg:EnableMouse(false)
        segments[i] = seg
    end
    layoutSegments()
    return true
end

local function updateSegments()
    if not active then
        return
    end
    hideStockManaVisual()
    for i = 1, 3 do
        local seg = segments[i]
        local pt = POWER_ORDER[i]
        local c = cache[pt]
        if seg and c then
            local maxv = c.max
            if maxv < 1 then
                maxv = 1
            end
            seg:SetMinMaxValues(0, maxv)
            seg:SetValue(c.cur)
            if seg.text then
                seg.text:SetText(math.floor(c.cur + 0.5) .. "/" .. math.floor(maxv + 0.5))
            end
        end
    end
end

local function activate()
    if not ensureSegments() then
        return
    end
    active = true
    for i = 1, 3 do
        segments[i]:Show()
    end
    layoutSegments()
    updateSegments()
end

local function deactivate()
    active = false
    for i = 1, 3 do
        if segments[i] then
            segments[i]:Hide()
        end
    end
    restoreStockManaVisual()
end

local function requestEnable()
    if SendAddonMessage then
        -- Prefer whisper-to-self so CMSG reaches the server as LANG_ADDON.
        local name = UnitName("player")
        if name then
            SendAddonMessage(PREFIX, "enable", "WHISPER", name)
        else
            SendAddonMessage(PREFIX, "enable")
        end
    end
end

local function handlePowerUpdate(body)
    -- PowerUpdate#powerType;current;total
    local rest = string.match(body, "^PowerUpdate#(.+)$")
    if not rest then
        return
    end
    local pt, cur, maxv = string.match(rest, "^(%d+);(%d+);(%d+)$")
    if not pt then
        return
    end
    pt = tonumber(pt)
    cur = tonumber(cur)
    maxv = tonumber(maxv)
    if not cache[pt] then
        cache[pt] = { cur = 0, max = 1 }
    end
    cache[pt].cur = cur
    cache[pt].max = maxv
    if active then
        updateSegments()
    end
end

local function handleAddonMessage(prefix, msg)
    if prefix ~= PREFIX then
        return
    end
    if msg == "AddonEnabled" then
        activate()
        return
    end
    handlePowerUpdate(msg)
end

frame:RegisterEvent("PLAYER_LOGIN")
frame:RegisterEvent("PLAYER_ENTERING_WORLD")
frame:RegisterEvent("CHAT_MSG_ADDON")

frame:SetScript("OnEvent", function(self, event, arg1, arg2, arg3, arg4)
    if event == "CHAT_MSG_ADDON" then
        -- arg1=prefix, arg2=message (2.4 CHAT_MSG_ADDON)
        handleAddonMessage(arg1, arg2)
        return
    end
    if event == "PLAYER_LOGIN" or event == "PLAYER_ENTERING_WORLD" then
        requestEnable()
    end
end)

frame:SetScript("OnUpdate", function(self, elapsed)
    self._t = (self._t or 0) + elapsed
    if self._t < 1.0 then
        return
    end
    self._t = 0
    if active then
        layoutSegments()
        updateSegments()
    elseif UnitExists("player") then
        -- Retry enable if login race missed the first reply.
        self._retries = (self._retries or 0) + 1
        if self._retries <= 5 then
            requestEnable()
        end
    end
end)
