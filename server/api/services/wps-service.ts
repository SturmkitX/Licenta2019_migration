import {DecodedPosition} from "../models/history-interm";
import * as XMLHttp from 'xmlhttprequest';

export class WpsService {
    constructor() {}

    private serverAddr: string = 'https://cps.combain.com?key=a7kyy6rye56jpke2nmm3';

    public getPosition(stations: any): DecodedPosition {
        let rawData = [];
        for (let station of stations) {
            rawData.push({macAddress: station.mac, signalStrength: station.rssi});
        }
        console.log('WPS raw data: ' + rawData);

        console.log('Requesting location...');
        let request = new XMLHttp.XMLHttpRequest();
        request.open("POST", this.serverAddr, false);
        request.setRequestHeader('Content-Type', 'application/json');
        request.send(JSON.stringify({wifiAccessPoints: rawData}));

        console.log(`Response: ${request.responseText}`);
        let position: DecodedPosition;
        try {
            position = JSON.parse(request.responseText);
        } catch {
            position = null;
        }
        return position;
    }
}