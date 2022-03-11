metadata {
	definition (name: "Tibber Monitor", namespace: "snisk", author: "sigval bergesen", mnmn: "SmartThingsCommunity", vid: "560e33c3-94b5-3b05-970c-de5af9ae5ae9", ocfDeviceType: "x.com.st.d.energymeter") {
        
        capability "Sensor"
        capability "Energy Meter" //workaround for Actions tiles etc
        capability "Refresh"
		
        capability "islandtravel33177.tibberPrice"
        capability "islandtravel33177.tibberPriceLevel"
	
        attribute "currency", "string"
	}

    preferences {
        input (
            name: "tibber_apikey",
            type: "password",
            title: "API Key",
            description: "Enter the Tibber API key",
            required: true,
            displayDuringSetup: true
        )
        input (
            name: "apkeylink",
            type: "link",
            title: "API Key can be found here:",
            description: '<a href="https://developer.tibber.com/settings/accesstoken">Tibber Accesstoken Token</a>'
        )
		input (
            name: "home",
            type: "number",
            title: "Home",
            description: "Enter the home you want to display, default is 0",
            required: true,
            displayDuringSetup: true,
        )
		input (
            name: "NORMAL",
            type: "paragraph",
            title: "NORMAL",
            description: "The price is greater than 90 % and smaller than 115 % compared to average price."
        )
        input (
            name: "CHEAP",
            type: "paragraph",
            title: "CHEAP",
            description: "The price is greater than 60 % and smaller or equal to 90 % compared to average price."
        )
        input (
            name: "VERY CHEAP",
            type: "paragraph",
            title: "VERY CHEAP",
            description: "The price is smaller or equal to 60 % compared to average price."
        )
        input (
            name: "EXPENSIVE",
            type: "paragraph",
            title: "EXPENSIVE",
            description: "The price is greater or equal to 115 % and smaller than 140 % compared to average price."
        )
        input (
            name: "VERY EXPENSIVE",
            type: "paragraph",
            title: "VERY EXPENSIVE",
            description: "The price is greater or equal to 140 % compared to average price."
        )
        input (
            name: "VERSION",
            type: "paragraph",
            title: "Version number",
            description: "customized x.1.0"
        )
    }
}

def homeNumber() {
		if(settings.home == null){
        return 0
        } else {
        return settings.home}
}        

def initialize() {
	state.price = 100;
	log.debug("init")
    getPrice()
    schedule("0 1,5,10,15,20,25,30 * * * ?", getPrice)
}

def installed() {
	log.debug "Installed"
    initialize()
}

def ping() {
    refresh()
}

def updated() {
	log.debug "Updated"
    initialize()
}

def refresh() {
    initialize()
}

def getPrice() {
	log.debug("getPrice")
    if(tibber_apikey == null){
        log.error("API key is not set. Please set it in the settings.")
    } else {
        def params = [
            uri: "https://api.tibber.com/v1-beta/gql",
            headers: ["Content-Type": "application/json;charset=UTF-8" , "Authorization": "Bearer $tibber_apikey"],
            body: '{"query": "{viewer {homes {address{address1} consumption(resolution: HOURLY, last: 1) {nodes {consumption consumptionUnit}} currentSubscription {priceInfo { current {total currency level} today {total startsAt} tomorrow{ total startsAt }}}}}}", "variables": null, "operationName": null}'
        ]
        try {
            httpPostJson(params) { resp ->
                if(resp.status == 200){

                    def currency = resp.data.data.viewer.homes[homeNumber()].currentSubscription.priceInfo.current.currency
                    def total = resp.data.data.viewer.homes[homeNumber()].currentSubscription.priceInfo.current.total
                    
                    state.currency = "${currencyToMinor(currency)}/kWh"
                    state.level = resp.data.data.viewer.homes[homeNumber()].currentSubscription.priceInfo.current.level
                    state.price = Math.round(total * 100)

					sendEvent(name: "energy", value: state.price, unit: currency)
                    sendEvent(name: "price", value: state.price, unit: state.currency)
                    sendEvent(name: "currency", value: state.currency)
                    sendEvent(name: "level", value: state.level)
                }
            }
        } catch (e) {
            log.debug "something went wrong: $e"
        }
    }
}

def parse(String description) {
    log.debug "parse description: ${description}"
    def eventMap = [
        createEvent(name: "energy", value: state.price, unit: state.currency),
        createEvent(name: "level", value: state.level),
        createEvent(name: "price", value: state.price, unit: state.currency),
        createEvent(name: "currencyLabel", value: state.currency, unit: state.currency)
    ]
    log.debug "Parse returned ${description}"
    return eventMap
}

def currencyToMinor(String currency){
	def currencyUnit = "";
	switch(currency){
    	case "NOK":currencyUnit = "Øre";break;
        case "SEK":currencyUnit = "Øre";break;
        case "USD":currencyUnit = "Penny";break;
        default: currencyUnit = "";break;
    }
    return currencyUnit;
    
}
