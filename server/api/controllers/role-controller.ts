import { UserRole } from '../models/user-role';
import { Request, Response } from 'express';
import * as bcrypt from 'bcryptjs';

export class RoleController{

    constructor() {}

    public getAll(req: Request, res: Response): void {
        UserRole.find({}, (err: any, roles: Document[]) => {
            if (err) {
                res.status(500).send(err);
            } else {
                res.status(200).json(roles);
            }
        });
    }

    public saveRole(req: Request, res: Response): void {
        if (!req.body || !req.body.role) {
            res.status(400).json(null);
        }

        const role = new UserRole({
            role: req.body.role
        }).save((err, roleSave) => {
            if (err) {
                res.status(500).send(err);
            } else {
                res.status(200).json(roleSave);
            }
        });
    }
}
