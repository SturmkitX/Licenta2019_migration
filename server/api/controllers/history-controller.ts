import { History } from '../models/history';
import { Tracker } from '../models/tracker';
import { Request, Response } from 'express';

export class HistoryController{

    constructor() {}

    /* ADMIN specific methods */

    public getAll(req: Request, res: Response): void {
        History.find()
            .exec((err: any, entries: Document[]) => {
                if (err) {
                    res.status(500).send(err);
                } else {
                    res.status(200).json(entries);
                }
            });
    }

    public getSpecific(req: Request, res: Response): void {
        History.findById(req.params.historyId, (err: any, entry: Document) => {
            if (err) {
                res.status(500).send(err);
            } else if (!entry) {
                res.status(404).json(null);
            } else {
                res.status(200).json(entry);
            }
        });
    }

    public saveHistory(req: Request, res: Response): void {
        if (!req.body) {
            res.status(400).json(null);
        }

        new History(req.body).save((err, entry) => {
            if (err) {
                res.status(500).send(err);
            } else {
                // update the parent tracker

                // @ts-ignore
                Tracker.findByIdAndUpdate(entry.trackerId, {$push: {history: entry._id}},
                    (err, tracker) => {
                    if (err) {
                        res.status(500).send(err);
                    }
                });
                res.status(200).json(entry);
            }
        });
    }


    /* USER + ADMIN methods */
    public getSelfForTracker(req: Request, res: Response): void {
        // the user specifies the id of a tracker
        // 1. check if the user owns that tracker
        // @ts-ignore
        const userId = req.user.id;
        Tracker.findOne({_id: req.params.trackerId, userId: userId})
            .populate('history')
            .exec((err: any, tracker: Document) => {
                if (err) {
                    res.status(500).send(err);
                } else if (!tracker) {
                    res.status(404).json([]);
                } else {
                    // @ts-ignore
                    res.status(200).json(tracker.history);
                }
            });
    }
}
