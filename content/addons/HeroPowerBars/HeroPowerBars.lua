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

local STAT_NAMES = { "Strength", "Agility", "Stamina", "Intellect", "Spirit" }
local statCache = { unspent = 0, spent = { 0, 0, 0, 0, 0 } }
local statPanel
local statButtons = {}
local unspentText

local function sendSpend(statIndex)
    if not SendAddonMessage then
        return
    end
    local name = UnitName("player")
    local body = "SpendStat;" .. (statIndex - 1) .. ";1"
    if name then
        SendAddonMessage(PREFIX, body, "WHISPER", name)
    else
        SendAddonMessage(PREFIX, body)
    end
end

local function refreshStatPanel()
    if not statPanel then
        return
    end
    if unspentText then
        unspentText:SetText("Unspent: " .. (statCache.unspent or 0))
    end
    for i = 1, 5 do
        local btn = statButtons[i]
        if btn then
            if btn.label then
                btn.label:SetText(STAT_NAMES[i] .. ": " .. (statCache.spent[i] or 0))
            end
            if (statCache.unspent or 0) < 1 then
                btn:Disable()
            else
                btn:Enable()
            end
        end
    end
end

local function ensureStatPanel()
    if statPanel or not PaperDollFrame then
        return statPanel ~= nil
    end
    local parent = PaperDollFrame
    statPanel = CreateFrame("Frame", ADDON .. "StatPanel", parent)
    statPanel:SetWidth(180)
    statPanel:SetHeight(150)
    statPanel:SetPoint("TOPLEFT", parent, "TOPRIGHT", -20, -40)
    local bg = statPanel:CreateTexture(nil, "BACKGROUND")
    bg:SetAllPoints(statPanel)
    bg:SetTexture("Interface\\Tooltips\\UI-Tooltip-Background")
    bg:SetVertexColor(0, 0, 0, 0.6)
    unspentText = statPanel:CreateFontString(nil, "OVERLAY", "GameFontNormal")
    unspentText:SetPoint("TOPLEFT", statPanel, "TOPLEFT", 8, -8)
    unspentText:SetText("Unspent: 0")
    for i = 1, 5 do
        local btn = CreateFrame("Button", ADDON .. "StatPlus" .. i, statPanel, "UIPanelButtonTemplate")
        btn:SetWidth(22)
        btn:SetHeight(18)
        btn:SetPoint("TOPLEFT", statPanel, "TOPLEFT", 150, -8 - i * 22)
        btn:SetText("+")
        btn:SetScript("OnClick", function()
            sendSpend(i)
        end)
        local label = statPanel:CreateFontString(nil, "OVERLAY", "GameFontHighlightSmall")
        label:SetPoint("LEFT", statPanel, "TOPLEFT", 8, -16 - i * 22)
        label:SetText(STAT_NAMES[i] .. ": 0")
        btn.label = label
        statButtons[i] = btn
    end
    statPanel:Show()
    refreshStatPanel()
    return true
end

local function handleStatUpdate(body)
    local rest = string.match(body, "^StatUpdate;(.+)$")
    if not rest then
        return
    end
    local u, s0, s1, s2, s3, s4 = string.match(rest, "^(%d+);(%d+);(%d+);(%d+);(%d+);(%d+)$")
    if not u then
        return
    end
    statCache.unspent = tonumber(u)
    statCache.spent[1] = tonumber(s0)
    statCache.spent[2] = tonumber(s1)
    statCache.spent[3] = tonumber(s2)
    statCache.spent[4] = tonumber(s3)
    statCache.spent[5] = tonumber(s4)
    ensureStatPanel()
    refreshStatPanel()
end

local function handleAddonMessage(prefix, msg)
    if prefix ~= PREFIX then
        return
    end
    if msg == "AddonEnabled" then
        activate()
        ensureStatPanel()
        return
    end
    handlePowerUpdate(msg)
    handleStatUpdate(msg)
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
