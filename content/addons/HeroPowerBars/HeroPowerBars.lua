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
-- Committed values from StatUpdate; draft is edited locally until Apply.
local statCache = { unspent = 0, spent = { 0, 0, 0, 0, 0 }, resetCopper = 0, resetCount = 0 }
local draftSpent = { 0, 0, 0, 0, 0 }
local draftUnspent = 0
local statPanel
local plusButtons = {}
local minusButtons = {}
local statLabels = {}
local unspentText
local costText
local applyBtn
local resetBtn

local function sendAddon(body)
    if not SendAddonMessage then
        return
    end
    local name = UnitName("player")
    if name then
        SendAddonMessage(PREFIX, body, "WHISPER", name)
    else
        SendAddonMessage(PREFIX, body)
    end
end

local function draftDirty()
    for i = 1, 5 do
        if (draftSpent[i] or 0) ~= (statCache.spent[i] or 0) then
            return true
        end
    end
    return false
end

local function syncDraftFromCache()
    draftUnspent = statCache.unspent or 0
    for i = 1, 5 do
        draftSpent[i] = statCache.spent[i] or 0
    end
end

local function formatCopper(copper)
    copper = tonumber(copper) or 0
    if copper <= 0 then
        return "Free"
    end
    local g = math.floor(copper / 10000)
    local s = math.floor((copper % 10000) / 100)
    local c = copper % 100
    if g > 0 then
        return g .. "g"
    end
    if s > 0 then
        return s .. "s"
    end
    return c .. "c"
end

local function refreshStatPanel()
    if not statPanel then
        return
    end
    if unspentText then
        unspentText:SetText("Unspent: " .. (draftUnspent or 0))
    end
    if costText then
        costText:SetText("Reset: " .. formatCopper(statCache.resetCopper))
    end
    for i = 1, 5 do
        if statLabels[i] then
            statLabels[i]:SetText(STAT_NAMES[i] .. ": " .. (draftSpent[i] or 0))
        end
        if plusButtons[i] then
            if (draftUnspent or 0) < 1 then
                plusButtons[i]:Disable()
            else
                plusButtons[i]:Enable()
            end
        end
        if minusButtons[i] then
            if (draftSpent[i] or 0) < 1 then
                minusButtons[i]:Disable()
            else
                minusButtons[i]:Enable()
            end
        end
    end
    if applyBtn then
        if draftDirty() then
            applyBtn:Enable()
        else
            applyBtn:Disable()
        end
    end
    if resetBtn then
        local committed = 0
        for i = 1, 5 do
            committed = committed + (statCache.spent[i] or 0)
        end
        if committed > 0 and not draftDirty() then
            resetBtn:Enable()
        else
            resetBtn:Disable()
        end
    end
end

local function draftPlus(statIndex)
    if (draftUnspent or 0) < 1 then
        return
    end
    draftSpent[statIndex] = (draftSpent[statIndex] or 0) + 1
    draftUnspent = draftUnspent - 1
    refreshStatPanel()
end

local function draftMinus(statIndex)
    if (draftSpent[statIndex] or 0) < 1 then
        return
    end
    draftSpent[statIndex] = draftSpent[statIndex] - 1
    draftUnspent = draftUnspent + 1
    refreshStatPanel()
end

local function sendApply()
    sendAddon("ApplyStats;"
        .. (draftSpent[1] or 0) .. ";"
        .. (draftSpent[2] or 0) .. ";"
        .. (draftSpent[3] or 0) .. ";"
        .. (draftSpent[4] or 0) .. ";"
        .. (draftSpent[5] or 0))
end

local function sendReset()
    sendAddon("ResetStats")
end

local function ensureStatPanel()
    if statPanel or not PaperDollFrame then
        return statPanel ~= nil
    end
    local parent = PaperDollFrame
    statPanel = CreateFrame("Frame", ADDON .. "StatPanel", parent)
    statPanel:SetWidth(200)
    statPanel:SetHeight(200)
    statPanel:SetPoint("TOPLEFT", parent, "TOPRIGHT", -20, -40)
    local bg = statPanel:CreateTexture(nil, "BACKGROUND")
    bg:SetAllPoints(statPanel)
    bg:SetTexture("Interface\\Tooltips\\UI-Tooltip-Background")
    bg:SetVertexColor(0, 0, 0, 0.6)
    unspentText = statPanel:CreateFontString(nil, "OVERLAY", "GameFontNormal")
    unspentText:SetPoint("TOPLEFT", statPanel, "TOPLEFT", 8, -8)
    unspentText:SetText("Unspent: 0")
    costText = statPanel:CreateFontString(nil, "OVERLAY", "GameFontHighlightSmall")
    costText:SetPoint("TOPRIGHT", statPanel, "TOPRIGHT", -8, -8)
    costText:SetText("Reset: Free")
    for i = 1, 5 do
        local y = -8 - i * 22
        local label = statPanel:CreateFontString(nil, "OVERLAY", "GameFontHighlightSmall")
        label:SetPoint("LEFT", statPanel, "TOPLEFT", 8, y - 8)
        label:SetText(STAT_NAMES[i] .. ": 0")
        statLabels[i] = label
        local minus = CreateFrame("Button", ADDON .. "StatMinus" .. i, statPanel, "UIPanelButtonTemplate")
        minus:SetWidth(22)
        minus:SetHeight(18)
        minus:SetPoint("TOPLEFT", statPanel, "TOPLEFT", 128, y)
        minus:SetText("-")
        minus:SetScript("OnClick", function()
            draftMinus(i)
        end)
        minusButtons[i] = minus
        local plus = CreateFrame("Button", ADDON .. "StatPlus" .. i, statPanel, "UIPanelButtonTemplate")
        plus:SetWidth(22)
        plus:SetHeight(18)
        plus:SetPoint("TOPLEFT", statPanel, "TOPLEFT", 152, y)
        plus:SetText("+")
        plus:SetScript("OnClick", function()
            draftPlus(i)
        end)
        plusButtons[i] = plus
    end
    applyBtn = CreateFrame("Button", ADDON .. "StatApply", statPanel, "UIPanelButtonTemplate")
    applyBtn:SetWidth(70)
    applyBtn:SetHeight(20)
    applyBtn:SetPoint("BOTTOMLEFT", statPanel, "BOTTOMLEFT", 8, 8)
    applyBtn:SetText("Apply")
    applyBtn:SetScript("OnClick", sendApply)
    resetBtn = CreateFrame("Button", ADDON .. "StatReset", statPanel, "UIPanelButtonTemplate")
    resetBtn:SetWidth(70)
    resetBtn:SetHeight(20)
    resetBtn:SetPoint("BOTTOMRIGHT", statPanel, "BOTTOMRIGHT", -8, 8)
    resetBtn:SetText("Reset")
    resetBtn:SetScript("OnClick", sendReset)
    syncDraftFromCache()
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
    syncDraftFromCache()
    ensureStatPanel()
    refreshStatPanel()
end

local function handleResetCost(body)
    local rest = string.match(body, "^ResetCost;(.+)$")
    if not rest then
        return
    end
    local copper, count = string.match(rest, "^(%d+);(%d+)$")
    if not copper then
        return
    end
    statCache.resetCopper = tonumber(copper)
    statCache.resetCount = tonumber(count)
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
    handleResetCost(msg)
end

frame:RegisterEvent("PLAYER_LOGIN")
frame:RegisterEvent("PLAYER_ENTERING_WORLD")
frame:RegisterEvent("PLAYER_LEVEL_UP")
frame:RegisterEvent("CHAT_MSG_ADDON")

frame:SetScript("OnEvent", function(self, event, arg1, arg2, arg3, arg4)
    if event == "CHAT_MSG_ADDON" then
        -- arg1=prefix, arg2=message (2.4 CHAT_MSG_ADDON)
        handleAddonMessage(arg1, arg2)
        return
    end
    if event == "PLAYER_LOGIN" or event == "PLAYER_ENTERING_WORLD" then
        requestEnable()
        return
    end
    if event == "PLAYER_LEVEL_UP" then
        -- Re-sync unspent points if the ding StatUpdate was missed.
        requestEnable()
    end
end)

-- When the character pane opens, build/refresh the allocation panel from cache.
if CharacterFrame then
    CharacterFrame:HookScript("OnShow", function()
        ensureStatPanel()
        refreshStatPanel()
    end)
end

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
