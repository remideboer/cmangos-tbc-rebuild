local ADDON = "CompanionPartyBar"
-- TBC SendAddonMessage rejects prefixes longer than 16 characters.
local PREFIX = "CompanionBar"
CompanionPartyBarDB = CompanionPartyBarDB or {}
CompanionPartyBarDB.loaded = true

local COMMAND_NAMES = {
    [1] = "Attack",
    [2] = "Follow",
    [3] = "Stay",
    [8] = "Aggressive",
    [9] = "Defensive",
    [10] = "Passive",
}

local COMMAND_ICONS = {
    [1] = "Interface\\Icons\\Ability_Hunter_Pet_Assist",
    [2] = "Interface\\Icons\\Ability_Tracking",
    [3] = "Interface\\Icons\\Ability_Hunter_Pet_Goto",
    [8] = "Interface\\Icons\\Ability_Racial_Cannibalize",
    [9] = "Interface\\Icons\\Ability_Defend",
    [10] = "Interface\\Icons\\Ability_Seal",
}

local events = CreateFrame("Frame", ADDON .. "Events")
local bar
local nameText
local buttons = {}
local slots = {}
local active = false
local stockUpdatePending = false
local syncElapsed = 0

local function send(body)
    if not SendAddonMessage then
        return
    end
    local player = UnitName("player")
    if player then
        SendAddonMessage(PREFIX, body, "WHISPER", player)
    else
        SendAddonMessage(PREFIX, body)
    end
end

local function requestState()
    CompanionPartyBarDB.requestCount = (CompanionPartyBarDB.requestCount or 0) + 1
    CompanionPartyBarDB.lastRequest = "enable"
    send("enable")
end

local function applyStockPetBar()
    if not PetActionBarFrame then
        return
    end
    if InCombatLockdown and InCombatLockdown() then
        stockUpdatePending = true
        return
    end
    stockUpdatePending = false
    if active then
        PetActionBarFrame:Hide()
    elseif UnitExists("pet") then
        PetActionBarFrame:Show()
    end
end

local function defaultPosition()
    bar:ClearAllPoints()
    if GetNumPartyMembers() > 0 and PartyMemberFrame1 then
        bar:SetPoint("TOPLEFT", UIParent, "TOPLEFT", 180, -180)
    else
        bar:SetPoint("TOPLEFT", UIParent, "TOPLEFT", 220, -110)
    end
end

local function restorePosition()
    if CompanionPartyBarDB.point then
        bar:ClearAllPoints()
        bar:SetPoint(
            CompanionPartyBarDB.point,
            UIParent,
            CompanionPartyBarDB.relativePoint,
            CompanionPartyBarDB.x,
            CompanionPartyBarDB.y
        )
    else
        defaultPosition()
    end
end

local function savePosition()
    local point, _, relativePoint, x, y = bar:GetPoint()
    CompanionPartyBarDB.point = point
    CompanionPartyBarDB.relativePoint = relativePoint
    CompanionPartyBarDB.x = x
    CompanionPartyBarDB.y = y
end

local function sendAction(slot)
    local target = UnitGUID("target") or "0"
    send("Action;" .. slot .. ";" .. target)
end

local function buttonTooltip(button)
    local slot = button.slot
    local data = slots[slot]
    if not data or data.action == 0 then
        return
    end
    GameTooltip:SetOwner(button, "ANCHOR_RIGHT")
    local command = COMMAND_NAMES[slot]
    if command then
        GameTooltip:SetText(command, 1, 1, 1)
        GameTooltip:AddLine("Companion command", 0.7, 0.7, 0.7)
    else
        local spellName, spellRank = GetSpellInfo(data.action)
        GameTooltip:SetText(spellName or ("Spell " .. data.action), 1, 1, 1)
        if spellRank and spellRank ~= "" then
            GameTooltip:AddLine(spellRank, 0.7, 0.7, 0.7)
        end
    end
    GameTooltip:Show()
end

local function createButton(slot)
    local button = CreateFrame("Button", ADDON .. "Button" .. slot, bar)
    button:SetWidth(38)
    button:SetHeight(38)
    button.slot = slot
    button:RegisterForClicks("LeftButtonUp")

    local icon = button:CreateTexture(nil, "ARTWORK")
    icon:SetPoint("TOPLEFT", button, "TOPLEFT", 3, -3)
    icon:SetPoint("BOTTOMRIGHT", button, "BOTTOMRIGHT", -3, 3)
    icon:SetTexCoord(0.07, 0.93, 0.07, 0.93)
    button.icon = icon

    local border = button:CreateTexture(nil, "OVERLAY")
    border:SetAllPoints(button)
    border:SetTexture("Interface\\Buttons\\UI-Quickslot2")

    local highlight = button:CreateTexture(nil, "HIGHLIGHT")
    highlight:SetPoint("TOPLEFT", button, "TOPLEFT", 3, -3)
    highlight:SetPoint("BOTTOMRIGHT", button, "BOTTOMRIGHT", -3, 3)
    highlight:SetTexture("Interface\\Buttons\\ButtonHilight-Square")
    highlight:SetBlendMode("ADD")

    local label = button:CreateFontString(nil, "OVERLAY", "GameFontNormalSmall")
    label:SetPoint("BOTTOM", button, "BOTTOM", 0, 4)
    button.label = label

    button:SetScript("OnClick", function()
        sendAction(slot)
    end)
    button:SetScript("OnEnter", buttonTooltip)
    button:SetScript("OnLeave", function()
        GameTooltip:Hide()
    end)
    return button
end

local function ensureBar()
    if bar then
        return
    end
    bar = CreateFrame("Frame", ADDON .. "Frame", UIParent)
    bar:SetWidth(214)
    bar:SetHeight(102)
    bar:SetFrameStrata("HIGH")
    bar:SetFrameLevel(20)
    bar:SetClampedToScreen(true)
    bar:SetMovable(true)
    bar:EnableMouse(true)
    bar:RegisterForDrag("LeftButton")

    local background = bar:CreateTexture(nil, "BACKGROUND")
    background:SetAllPoints(bar)
    background:SetTexture(0, 0, 0)
    background:SetAlpha(0.72)

    nameText = bar:CreateFontString(nil, "OVERLAY", "GameFontNormal")
    nameText:SetPoint("TOP", bar, "TOP", 0, -7)
    nameText:SetText("Companion")

    bar:SetScript("OnDragStart", function()
        if not (InCombatLockdown and InCombatLockdown()) then
            bar:StartMoving()
        end
    end)
    bar:SetScript("OnDragStop", function()
        bar:StopMovingOrSizing()
        savePosition()
    end)

    for slot = 1, 10 do
        local button = createButton(slot)
        local column = math.mod(slot - 1, 5)
        local row = math.floor((slot - 1) / 5)
        button:SetPoint("TOPLEFT", bar, "TOPLEFT", 7 + column * 41, -22 - row * 39)
        buttons[slot] = button
    end
    restorePosition()
    bar:Hide()
end

local function updateButton(slot)
    local button = buttons[slot]
    local data = slots[slot]
    local command = COMMAND_NAMES[slot]
    if not button or not data then
        return
    end
    if command then
        button.icon:SetTexture(COMMAND_ICONS[slot])
        button.label:SetText(command)
        button:Enable()
        button.icon:SetVertexColor(1, 1, 1)
    elseif data.action ~= 0 then
        local spellName, _, spellIcon = GetSpellInfo(data.action)
        button.icon:SetTexture(spellIcon or "Interface\\Icons\\INV_Misc_QuestionMark")
        button.label:SetText(spellName or data.action)
        button:Enable()
        button.icon:SetVertexColor(1, 1, 1)
    else
        button.icon:SetTexture("Interface\\Buttons\\UI-Quickslot")
        button.label:SetText("")
        button:Disable()
        button.icon:SetVertexColor(0.35, 0.35, 0.35)
    end
end

local function deactivate()
    active = false
    if bar then
        bar:Hide()
    end
    applyStockPetBar()
end

local function splitState(message)
    local fields = {}
    local start = 1
    while true do
        local separator = string.find(message, ";", start, true)
        if not separator then
            table.insert(fields, string.sub(message, start))
            return fields
        end
        table.insert(fields, string.sub(message, start, separator - 1))
        start = separator + 1
    end
end

local function handleState(message)
    CompanionPartyBarDB.lastState = message
    local fields = splitState(message)
    if fields[1] ~= "State" then
        return
    end
    ensureBar()
    if fields[2] ~= "1" then
        deactivate()
        return
    end
    if table.getn(fields) ~= 13 then
        return
    end
    for slot = 1, 10 do
        local action, actionType = string.match(fields[slot + 3], "^(%d+),(%d+)$")
        if not action then
            return
        end
        slots[slot] = {
            action = tonumber(action),
            actionType = tonumber(actionType),
        }
    end
    active = true
    syncElapsed = 0
    nameText:SetText(fields[3])
    for slot = 1, 10 do
        updateButton(slot)
    end
    bar:SetAlpha(1)
    bar:Show()
    applyStockPetBar()
end

SLASH_COMPANIONPARTYBAR1 = "/cpb"
SlashCmdList.COMPANIONPARTYBAR = function(command)
    command = string.lower(command or "")
    ensureBar()
    if command == "reset" then
        CompanionPartyBarDB = {}
        CompanionPartyBarDB.loaded = true
        defaultPosition()
        DEFAULT_CHAT_FRAME:AddMessage("CompanionPartyBar position reset.")
    elseif command == "show" then
        CompanionPartyBarDB.point = nil
        defaultPosition()
        bar:SetAlpha(1)
        bar:Show()
        DEFAULT_CHAT_FRAME:AddMessage("CompanionPartyBar forced on screen.")
    elseif command == "status" then
        DEFAULT_CHAT_FRAME:AddMessage(
            "CompanionPartyBar loaded=yes active=" .. tostring(active)
            .. " shown=" .. tostring(bar:IsShown())
            .. " left=" .. tostring(bar:GetLeft())
            .. " top=" .. tostring(bar:GetTop())
            .. " requests=" .. tostring(CompanionPartyBarDB.requestCount or 0)
            .. " lastState=" .. tostring(CompanionPartyBarDB.lastState or "none")
        )
    else
        DEFAULT_CHAT_FRAME:AddMessage(
            "CompanionPartyBar: drag to move; /cpb show forces visibility; /cpb status shows sync state."
        )
    end
end

events:RegisterEvent("PLAYER_LOGIN")
events:RegisterEvent("PLAYER_ENTERING_WORLD")
events:RegisterEvent("PLAYER_REGEN_ENABLED")
events:RegisterEvent("UNIT_PET")
events:RegisterEvent("PET_BAR_UPDATE")
events:RegisterEvent("PARTY_MEMBERS_CHANGED")
events:RegisterEvent("CHAT_MSG_ADDON")

events:SetScript("OnEvent", function(_, event, arg1, arg2)
    if event == "CHAT_MSG_ADDON" then
        if arg1 == PREFIX then
            handleState(arg2)
        end
        return
    end
    if event == "PLAYER_REGEN_ENABLED" then
        if stockUpdatePending then
            applyStockPetBar()
        end
        return
    end
    if event == "UNIT_PET" and arg1 ~= "player" then
        return
    end
    ensureBar()
    requestState()
end)

events:SetScript("OnUpdate", function(self, elapsed)
    self.elapsed = (self.elapsed or 0) + elapsed
    syncElapsed = syncElapsed + elapsed
    if self.elapsed < 0.5 then
        return
    end
    self.elapsed = 0
    if not active and syncElapsed >= 2 then
        syncElapsed = 0
        requestState()
    end
    if active and PetActionBarFrame and PetActionBarFrame:IsShown()
            and not (InCombatLockdown and InCombatLockdown()) then
        PetActionBarFrame:Hide()
    end
end)
