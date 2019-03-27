class HistoryInterm {
    public trackerId: string;
    public lat: number;
    public lng: number;
    public range: number;
    public source: string;
}

class DecodedPosition {
    public result: number;
    public data: DecodedData;
}

class DecodedData {
    public lat: number;
    public lon: number;
    public range: number;
    public time: number;
}

export {HistoryInterm, DecodedPosition};